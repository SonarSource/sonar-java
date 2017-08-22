/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

@Rule(key = "S4200")
public class WrappedNativeMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree method = (MethodTree) tree;
    if (isPublic(method) && isNativeMethod(method)) {
      reportIssue(method, "Make this native method private and provide a wrapper.");
    } else {
      checkForTrivialWrapper(method);
    }
  }

  private static boolean isPublic(MethodTree method) {
    return ModifiersUtils.hasModifier(method.modifiers(), Modifier.PUBLIC);
  }

  private static boolean isNativeMethod(@Nullable MethodTree method) {
    return method != null && ModifiersUtils.hasModifier(method.modifiers(), Modifier.NATIVE);
  }

  private void checkForTrivialWrapper(MethodTree wrapper) {
    BlockTree block = wrapper.block();
    if (block == null) {
      // abstract methods
      return;
    }
    List<StatementTree> body = block.body();
    if (body.size() != 1) {
      // not trivial - empty body or more than one statement
      return;
    }
    MethodInvocationTree mit = getMethodInvocation(body.get(0));
    if (!isOnlyUsingWrapperParametersInMethodInvocation(mit, wrapper.symbol())) {
      // not trivial - operation on parameters
      return;
    }
    MethodTree methodDeclaration = (MethodTree) mit.symbol().declaration();
    if (isNativeMethod(methodDeclaration)) {
      reportIssue(wrapper.simpleName(),
        "Make this wrapper for native method '" + methodDeclaration.simpleName().name() + "' less trivial.",
        Collections.singletonList(new JavaFileScannerContext.Location("", methodDeclaration)),
        null);
    }
  }

  @CheckForNull
  private static MethodInvocationTree getMethodInvocation(StatementTree statementTree) {
    ExpressionTree expressionTree = null;
    if (statementTree.is(Tree.Kind.RETURN_STATEMENT)) {
      expressionTree = ((ReturnStatementTree) statementTree).expression();
    } else if (statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      expressionTree = ((ExpressionStatementTree) statementTree).expression();
    }
    if (expressionTree == null) {
      return null;
    }
    expressionTree = ExpressionUtils.skipParentheses(expressionTree);
    if (!expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return null;
    }
    return (MethodInvocationTree) expressionTree;
  }

  private static boolean isOnlyUsingWrapperParametersInMethodInvocation(@Nullable MethodInvocationTree mit, Symbol.MethodSymbol wrapperSymbol) {
    return mit != null && mit.arguments().stream().allMatch(parameter -> isMethodParameter(parameter, wrapperSymbol));
  }

  private static boolean isMethodParameter(ExpressionTree parameter, Symbol.MethodSymbol wrapperSymbol) {
    return parameter.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) parameter).symbol().owner() == wrapperSymbol;
  }
}
