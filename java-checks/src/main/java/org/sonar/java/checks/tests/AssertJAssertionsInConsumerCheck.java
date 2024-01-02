/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.AbstractAssertionVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static java.util.Collections.singletonList;

@Rule(key = "S6103")
public class AssertJAssertionsInConsumerCheck extends IssuableSubscriptionVisitor {

  private static final String ORG_ASSERTJ_CORE_API_ABSTRACT_ASSERT = "org.assertj.core.api.AbstractAssert";
  private static final String JAVA_UTIL_FUNCTION_CONSUMER = "java.util.function.Consumer";
  private static final String ORG_ASSERTJ_CORE_API_THROWING_CONSUMER = "org.assertj.core.api.ThrowingConsumer";
  private static final String ORG_ASSERTJ_CORE_API_THROWING_CONSUMER_ARRAY = "org.assertj.core.api.ThrowingConsumer[]";

  private static final MethodMatchers METHODS_WITH_CONSUMER_AT_INDEX_0_MATCHER = MethodMatchers.create()
    .ofSubTypes(ORG_ASSERTJ_CORE_API_ABSTRACT_ASSERT)
    .names("allSatisfy", "anySatisfy", "hasOnlyOneElementSatisfying", "noneSatisfy", "satisfies")
    .addParametersMatcher(JAVA_UTIL_FUNCTION_CONSUMER)
    .addParametersMatcher(ORG_ASSERTJ_CORE_API_THROWING_CONSUMER)
    .addParametersMatcher(ORG_ASSERTJ_CORE_API_THROWING_CONSUMER_ARRAY)
    .addParametersMatcher(JAVA_UTIL_FUNCTION_CONSUMER, "org.assertj.core.data.Index")
    .build();

  private static final MethodMatchers METHODS_WITH_CONSUMER_AT_INDEX_1_MATCHER = MethodMatchers.create()
    .ofSubTypes(ORG_ASSERTJ_CORE_API_ABSTRACT_ASSERT)
    .names("isInstanceOfSatisfying", "zipSatisfy")
    .addParametersMatcher(arguments -> arguments.size() > 1 && (arguments.get(1).is(JAVA_UTIL_FUNCTION_CONSUMER) || arguments.get(1).is("java.util.function.BiConsumer")))
    .build();

  private static final MethodMatchers SATISFIES_ANY_OF_MATCHER = MethodMatchers.create()
    .ofSubTypes(ORG_ASSERTJ_CORE_API_ABSTRACT_ASSERT)
    .names("satisfiesAnyOf")
    .withAnyParameters()
    .build();

  private final Map<Symbol, Boolean> assertionInLocalMethod = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (METHODS_WITH_CONSUMER_AT_INDEX_0_MATCHER.matches(invocation)) {
      checkAssertions(invocation, singletonList(invocation.arguments().get(0)));
    } else if (METHODS_WITH_CONSUMER_AT_INDEX_1_MATCHER.matches(invocation)) {
      checkAssertions(invocation, singletonList(invocation.arguments().get(1)));
    } else if (SATISFIES_ANY_OF_MATCHER.matches(invocation)) {
      checkAssertions(invocation, invocation.arguments());
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    assertionInLocalMethod.clear();
    super.leaveFile(context);
  }

  private void checkAssertions(MethodInvocationTree invocation, List<ExpressionTree> argumentsToCheck) {
    List<Location> argumentsMissingAssertion = argumentsToCheck.stream()
      .filter(argument -> !hasAssertion(argument))
      .map(argument -> new Location("Argument missing assertion", argument))
      .collect(Collectors.toList());

    if (!argumentsMissingAssertion.isEmpty()) {
      IdentifierTree methodName = ExpressionUtils.methodName(invocation);
      reportIssue(
        methodName,
        "Rework this assertion to assert something inside the Consumer argument.",
        argumentsMissingAssertion,
        null);
    }
  }

  private boolean hasAssertion(@Nullable ExpressionTree expressionTree) {
    // if the expression to check cannot be resolved, we assume it has assertions to avoid FP
    if (expressionTree == null) {
      return true;
    }

    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      Tree argumentDeclaration = ((IdentifierTree) expressionTree).symbol().declaration();
      return argumentDeclaration instanceof VariableTree && hasAssertion(((VariableTree) argumentDeclaration).initializer());
    } else {
      AssertionVisitor assertionVisitor = new AssertionVisitor();
      expressionTree.accept(assertionVisitor);
      return assertionVisitor.hasAssertion();
    }
  }

  private class AssertionVisitor extends AbstractAssertionVisitor {
    @Override
    protected boolean isAssertion(Symbol methodSymbol) {
      if (!assertionInLocalMethod.containsKey(methodSymbol)) {
        assertionInLocalMethod.put(methodSymbol, false);
        Tree declaration = methodSymbol.declaration();
        if (declaration != null) {
          AssertionVisitor assertionVisitor = new AssertionVisitor();
          declaration.accept(assertionVisitor);
          assertionInLocalMethod.put(methodSymbol, assertionVisitor.hasAssertion());
        }
      }

      return assertionInLocalMethod.get(methodSymbol);
    }
  }

}
