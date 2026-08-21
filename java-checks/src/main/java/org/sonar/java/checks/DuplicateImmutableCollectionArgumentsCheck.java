/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9361")
public class DuplicateImmutableCollectionArgumentsCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_TEMPLATE = "Remove or %s this duplicate %s; \"%s\" throws an \"IllegalArgumentException\" at runtime when %s are duplicated.";
  private static final String MAP_OF_MESSAGE = String.format(MESSAGE_TEMPLATE, "rename", "key", "Map.of", "keys");
  private static final String MAP_OF_ENTRIES_MESSAGE = String.format(MESSAGE_TEMPLATE, "rename", "key", "Map.ofEntries", "keys");
  private static final String SET_OF_MESSAGE = String.format(MESSAGE_TEMPLATE, "replace", "element", "Set.of", "elements");

  private static final String FIRST_KEY_SECONDARY_MESSAGE = "First occurrence of this key.";
  private static final String FIRST_ELEMENT_SECONDARY_MESSAGE = "First occurrence of this element.";

  private static final MethodMatchers MAP_OF = MethodMatchers.create()
    .ofTypes("java.util.Map")
    .names("of")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MAP_OF_ENTRIES = MethodMatchers.create()
    .ofTypes("java.util.Map")
    .names("ofEntries")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MAP_ENTRY = MethodMatchers.create()
    .ofTypes("java.util.Map")
    .names("entry")
    .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  private static final MethodMatchers SET_OF = MethodMatchers.create()
    .ofTypes("java.util.Set")
    .names("of")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (MAP_OF.matches(mit)) {
      checkMapOf(mit);
    } else if (MAP_OF_ENTRIES.matches(mit)) {
      checkMapOfEntries(mit);
    } else if (SET_OF.matches(mit)) {
      checkSetOf(mit);
    }
  }

  private void checkMapOf(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    List<ExpressionTree> keys = new ArrayList<>();
    for (int i = 0; i < arguments.size(); i += 2) {
      keys.add(ExpressionUtils.skipParentheses(arguments.get(i)));
    }
    checkDuplicates(keys, MAP_OF_MESSAGE, FIRST_KEY_SECONDARY_MESSAGE);
  }

  private void checkMapOfEntries(MethodInvocationTree mit) {
    List<ExpressionTree> keys = new ArrayList<>();
    for (ExpressionTree arg : mit.arguments()) {
      ExpressionTree unwrapped = ExpressionUtils.skipParentheses(arg);
      if (unwrapped.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree entryMit = (MethodInvocationTree) unwrapped;
        if (MAP_ENTRY.matches(entryMit) && entryMit.arguments().size() == 2) {
          keys.add(ExpressionUtils.skipParentheses(entryMit.arguments().get(0)));
        }
      }
    }
    checkDuplicates(keys, MAP_OF_ENTRIES_MESSAGE, FIRST_KEY_SECONDARY_MESSAGE);
  }

  private void checkSetOf(MethodInvocationTree mit) {
    List<ExpressionTree> elements = new ArrayList<>();
    for (ExpressionTree arg : mit.arguments()) {
      elements.add(ExpressionUtils.skipParentheses(arg));
    }
    checkDuplicates(elements, SET_OF_MESSAGE, FIRST_ELEMENT_SECONDARY_MESSAGE);
  }

  private void checkDuplicates(List<ExpressionTree> expressions, String message, String secondaryMessage) {
    List<ExpressionTree> seen = new ArrayList<>();
    for (ExpressionTree expr : expressions) {
      ExpressionTree firstOccurrence = findFirstEquivalent(seen, expr);
      if (firstOccurrence != null) {
        reportIssue(
          expr,
          message,
          Collections.singletonList(new JavaFileScannerContext.Location(secondaryMessage, firstOccurrence)),
          null
        );
      } else {
        seen.add(expr);
      }
    }
  }

  private static ExpressionTree findFirstEquivalent(List<ExpressionTree> seen, ExpressionTree target) {
    for (ExpressionTree prior : seen) {
      if (areEquivalent(prior, target)) {
        return prior;
      }
    }
    return null;
  }

  private static boolean areEquivalent(ExpressionTree expr1, ExpressionTree expr2) {
    ExpressionTree e1 = resolveExpression(expr1);
    ExpressionTree e2 = resolveExpression(expr2);

    Optional<Object> const1 = e1.asConstant();
    Optional<Object> const2 = e2.asConstant();
    if (const1.isPresent() && const2.isPresent()) {
      return const1.get().equals(const2.get());
    }

    String str1 = ExpressionsHelper.getConstantValueAsString(e1).value();
    String str2 = ExpressionsHelper.getConstantValueAsString(e2).value();
    if (str1 != null && str2 != null) {
      return str1.equals(str2);
    }

    Boolean bool1 = ExpressionsHelper.getConstantValueAsBoolean(e1).value();
    Boolean bool2 = ExpressionsHelper.getConstantValueAsBoolean(e2).value();
    if (bool1 != null && bool2 != null) {
      return bool1.equals(bool2);
    }

    return ExpressionsHelper.alwaysReturnSameValue(e1)
      && ExpressionsHelper.alwaysReturnSameValue(e2)
      && SyntacticEquivalence.areEquivalentIncludingSameVariables(e1, e2);
  }

  private static ExpressionTree resolveExpression(ExpressionTree expression) {
    return resolveExpression(expression, new HashSet<>());
  }

  private static ExpressionTree resolveExpression(ExpressionTree expression, Set<Symbol> visited) {
    ExpressionTree current = ExpressionUtils.skipParentheses(expression);
    if (current.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) current).symbol();
      if (!symbol.isUnknown() && visited.add(symbol)) {
        ExpressionTree singleWrite = ExpressionsHelper.getSingleWriteUsage(symbol);
        if (singleWrite != null) {
          return resolveExpression(singleWrite, visited);
        }
      }
    }
    return current;
  }
}
