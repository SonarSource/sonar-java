/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8432")
public class UnusedScopedValueWhereResultCheck extends IssuableSubscriptionVisitor {

  private static final String WHERE_METHOD_NAME = "where";

  private static final String MESSAGE = "Use this ScopedValue.Carrier by calling run() or call(), or remove this useless call to where().";

  private static final Set<String> CONSUMPTION_METHODS = Set.of("run", "call");

  private static final MethodMatchers WHERE_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.lang.ScopedValue")
      .names(WHERE_METHOD_NAME)
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.lang.ScopedValue$Carrier")
      .names(WHERE_METHOD_NAME)
      .withAnyParameters()
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof MethodInvocationTree methodInvocationTree) {
      checkMethodInvocation(methodInvocationTree);
    } else if (tree instanceof VariableTree variableTree) {
      checkCarrierVariable(variableTree);
    }
  }

  private void checkMethodInvocation(MethodInvocationTree mit) {
    if (!WHERE_MATCHER.matches(mit)) {
      return;
    }

    Tree parent = mit.parent();

    // Special case: if there is no parent, consider it consumed to avoid false positives
    if (parent == null) {
      return;
    }

    // Check if result is immediately consumed (chained .run() or .call())
    if (isImmediatelyConsumed(parent)) {
      return;
    }

    // Check if result is chained with another .where() - don't report, the final result will be checked
    if (isChainedWhere(parent)) {
      return;
    }

    // Check if result is assigned to a variable - the variable check will handle this
    if (parent instanceof VariableTree) {
      return;
    }

    // Check if result is assigned to a field (escapes)
    if (parent instanceof AssignmentExpressionTree) {
      return;
    }

    // Check if result escapes (returned or passed as argument)
    if (isEscaping(mit)) {
      return;
    }

    // If we reach here, the result is discarded
    reportIssue(mit, MESSAGE);
  }

  private void checkCarrierVariable(VariableTree variableTree) {
    Symbol symbol = variableTree.symbol();
    if (!symbol.isVariableSymbol()) {
      return;
    }

    // Check if this variable is initialized with a where() call
    var initializer = variableTree.initializer();
    boolean isWhereResult = initializer instanceof MethodInvocationTree mit && WHERE_MATCHER.matches(mit);

    // Check if this variable is a Carrier type parameter
    boolean isCarrierParameter = symbol.isParameter() && isCarrierType(symbol);

    if (!isWhereResult && !isCarrierParameter) {
      return;
    }

    // Check all usages of the variable
    Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) symbol;
    List<IdentifierTree> usages = variableSymbol.usages();

    // Check if variable is reassigned before proper use
    if (isWhereResult && isReassignedBeforeProperUse(usages)) {
      reportIssue(variableTree.simpleName(), MESSAGE);
      return;
    }

    boolean isProperlyUsed = usages.stream().anyMatch(this::isUsageValid);

    if (!isProperlyUsed) {
      reportIssue(variableTree.simpleName(), MESSAGE);
    }
  }

  private boolean isReassignedBeforeProperUse(List<IdentifierTree> usages) {
    for (IdentifierTree usage : usages) {
      // Check if this usage is a reassignment (left side of assignment)
      Tree parent = usage.parent();
      if (parent instanceof AssignmentExpressionTree assignment && assignment.variable() == usage) {
        // This is a reassignment - check if there's any proper use before this reassignment
        // For simplicity, if we see a reassignment and no proper use, consider it unused
        boolean hasProperUseBefore = usages.stream()
          .filter(u -> u != usage)
          .filter(u -> isBefore(u, usage))
          .anyMatch(this::isUsageValid);
        if (!hasProperUseBefore) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isBefore(IdentifierTree first, IdentifierTree second) {
    return first.identifierToken().range().start().isBefore(second.identifierToken().range().start());
  }

  private static boolean isCarrierType(Symbol symbol) {
    return symbol.type().is("java.lang.ScopedValue$Carrier");
  }

  private boolean isUsageValid(IdentifierTree usage) {
    Tree parent = usage.parent();

    // Check if usage is a reassignment (left side of assignment) - not a valid use
    if (parent instanceof AssignmentExpressionTree assignment && assignment.variable() == usage) {
      return false;
    }

    // Check if usage is consumed via .run() or .call()
    if (parent instanceof MemberSelectExpressionTree memberSelect && memberSelect.expression() == usage) {
      String methodName = memberSelect.identifier().name();
      if (CONSUMPTION_METHODS.contains(methodName)) {
        return true;
      }
      // Check if it's a chained .where() that eventually gets consumed
      if (WHERE_METHOD_NAME.equals(methodName)) {
        Tree grandParent = memberSelect.parent();
        if (grandParent instanceof MethodInvocationTree chainedMit) {
          return isChainEventuallyConsumed(chainedMit);
        }
      }
    }

    // Check if usage is assigned to another variable (aliasing)
    if (parent instanceof VariableTree varTree) {
      Symbol targetSymbol = varTree.symbol();
      // If assigned to a field, it escapes
      if (!targetSymbol.isLocalVariable()) {
        return true;
      }
      // If assigned to a local variable, check if that variable is properly used
      if (targetSymbol.isVariableSymbol()) {
        return targetSymbol.usages().stream().anyMatch(this::isUsageValid);
      }
    }

    // Check if usage escapes (returned or passed as argument)
    return isEscaping(usage);
  }

  private static boolean isImmediatelyConsumed(Tree parent) {
    if (parent instanceof MemberSelectExpressionTree memberSelect) {
      return CONSUMPTION_METHODS.contains(memberSelect.identifier().name());
    }
    return false;
  }

  private static boolean isChainedWhere(Tree parent) {
    if (parent instanceof MemberSelectExpressionTree memberSelect) {
      return WHERE_METHOD_NAME.equals(memberSelect.identifier().name());
    }
    return false;
  }

  private boolean isChainEventuallyConsumed(MethodInvocationTree mit) {
    Tree parent = mit.parent();

    // Special case: if there is no parent, consider it consumed to avoid false positives
    if (parent == null) {
      return true;
    }

    if (isImmediatelyConsumed(parent)) {
      return true;
    }

    if (isChainedWhere(parent) && parent.parent() instanceof MethodInvocationTree nextMit) {
      return isChainEventuallyConsumed(nextMit);
    }

    if (isEscaping(mit)) {
      return true;
    }

    if (parent instanceof VariableTree varTree) {
      Symbol symbol = varTree.symbol();
      if (symbol.isVariableSymbol()) {
        return symbol.usages().stream().anyMatch(this::isUsageValid);
      }
    }

    return false;
  }

  private static boolean isEscaping(Tree tree) {
    Tree parent = tree.parent();

    if (parent == null) {
      return false;
    }

    // Returned from method
    if (parent instanceof ReturnStatementTree) {
      return true;
    }

    // Passed as argument to another method or constructor
    if (parent instanceof Arguments) {
      return true;
    }

    // Used in ternary expression - check if the ternary escapes
    if (parent instanceof ConditionalExpressionTree) {
      return isEscaping(parent);
    }

    // Passed to constructor
    if (parent instanceof NewClassTree newClass) {
      return newClass.arguments().contains(tree);
    }

    return false;
  }
}


