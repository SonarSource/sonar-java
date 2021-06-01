/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S6073")
public class MockitoArgumentMatchersUsedOnAllParametersCheck extends AbstractMockitoArgumentChecker {
  private static final String ARGUMENT_CAPTOR_CLASS = "org.mockito.ArgumentCaptor";
  private static final String ARGUMENT_MATCHER_CLASS = "org.mockito.ArgumentMatchers";
  private static final String ADDITIONAL_MATCHER_CLASS = "org.mockito.AdditionalMatchers";
  private static final String OLD_MATCHER_CLASS = "org.mockito.Matchers";
  private static final String TOP_MOCKITO_CLASS = "org.mockito.Mockito";

  // Argument matchers are not filtered on names but the class they originate from to support the addition of new matchers.
  private static final MethodMatchers ARGUMENT_MARCHER = MethodMatchers.create()
    .ofTypes(ARGUMENT_MATCHER_CLASS, ADDITIONAL_MATCHER_CLASS, OLD_MATCHER_CLASS, TOP_MOCKITO_CLASS)
    .anyName()
    .withAnyParameters()
    .build();

  private static final MethodMatchers ARGUMENT_CAPTOR = MethodMatchers.create()
    .ofTypes(ARGUMENT_CAPTOR_CLASS)
    .names("capture")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    MethodVisitor.cachedResults.clear();
  }

  @Override
  protected void visitArguments(Arguments arguments) {
    if (arguments.isEmpty()) {
      return;
    }
    List<Tree> nonMatchers = new ArrayList<>();
    for (ExpressionTree arg : arguments) {
      arg = ExpressionUtils.skipParentheses(arg);
      if (!isArgumentMatcherLike(arg)) {
        nonMatchers.add(arg);
      }
    }
    int nonMatchersFound = nonMatchers.size();

    if (!nonMatchers.isEmpty() && nonMatchersFound < arguments.size()) {
      String primaryMessage = String.format(
        "Add an \"eq()\" argument matcher on %s",
        nonMatchersFound == 1 ? "this parameter." : "these parameters."
      );
      reportIssue(nonMatchers.get(0),
        primaryMessage,
        nonMatchers.stream()
          .skip(1)
          .map(secondary -> new JavaFileScannerContext.Location("", secondary))
          .collect(Collectors.toList()),
        null);
    }
  }

  private static boolean isArgumentMatcherLike(ExpressionTree tree) {
    ExpressionTree unpacked = skipCasts(tree);
    if (!unpacked.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    MethodInvocationTree invocation = (MethodInvocationTree) unpacked;
    return ARGUMENT_CAPTOR.matches(invocation) ||
      ARGUMENT_MARCHER.matches(invocation) ||
      returnsAnArgumentMatcher(invocation);
  }

  /**
   * Test whether an invoked method eventually returns an argument matcher by checking if all its return paths lead to another method invocation.
   * The return method invocations are not checked as they are most likely stored in some testing helper out of the file under analysis.
   * @param invocation The method invocation to explore
   * @return Whether the method invoked returns something that could be an argument matcher
   */
  private static boolean returnsAnArgumentMatcher(MethodInvocationTree invocation) {
    ExpressionTree methodSelect = invocation.methodSelect();
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) methodSelect;
      Symbol symbol = identifier.symbol();
      if (symbol.isUnknown()) {
        return true;
      }
      Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
      MethodTree declaration = methodSymbol.declaration();
      if (declaration == null) {
        return false;
      }
      MethodVisitor methodVisitor = new MethodVisitor();
      declaration.accept(methodVisitor);
      return methodVisitor.onlyReturnsMethodInvocations;
    }
    return false;
  }

  /**
   * Pop the chained casts to return an expression.
   * @param tree Chained casts
   * @return The expression behind the last cast in the chain
   */
  private static ExpressionTree skipCasts(ExpressionTree tree) {
    ExpressionTree current = ExpressionUtils.skipParentheses(tree);
    while (current.is(Tree.Kind.TYPE_CAST)) {
      TypeCastTree cast = (TypeCastTree) current;
      current = ExpressionUtils.skipParentheses(cast.expression());
    }
    return current;
  }

  private static class MethodVisitor extends BaseTreeVisitor {
    static Map<MethodTree, Boolean> cachedResults = new HashMap<>();
    boolean onlyReturnsMethodInvocations = false;

    @Override
    public void visitMethod(MethodTree tree) {
      if (cachedResults.containsKey(tree)) {
        onlyReturnsMethodInvocations = cachedResults.get(tree);
        return;
      }
      cachedResults.put(tree, Boolean.FALSE);
      onlyReturnsMethodInvocations = tree.block().body().stream()
        .filter(statement -> statement.is(Tree.Kind.RETURN_STATEMENT))
        .map(ReturnStatementTree.class::cast)
        .map(ReturnStatementTree::expression)
        .allMatch(expression -> expression.is(Tree.Kind.METHOD_INVOCATION));
      cachedResults.put(tree, onlyReturnsMethodInvocations);
    }
  }
}
