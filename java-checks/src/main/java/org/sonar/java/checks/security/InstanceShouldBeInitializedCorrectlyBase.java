/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.security;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class InstanceShouldBeInitializedCorrectlyBase extends IssuableSubscriptionVisitor {

  private final List<VariableSymbol> correctlyInitializedViaConstructor = Lists.newArrayList();
  private final List<VariableSymbol> variablesToFlag = Lists.newArrayList();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  protected abstract String getMessage();

  protected abstract boolean isCorrectlyInitializedViaConstructor(VariableTree variableSymbol);

  protected abstract String getMethodName();

  protected abstract boolean methodArgumentsHaveExpectedValue(Arguments arguments);

  protected abstract int getMethodArity();

  protected abstract List<String> getClasses();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    variablesToFlag.clear();
    super.scanFile(context);
    for (Symbol.VariableSymbol var : variablesToFlag) {
      reportIssue(var.declaration().simpleName(), getMessage());
    }
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        addToVariablesToFlag(variableTree);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        removeFromVariablesToFlagIfInitializedWithMethod(mit);
      }
    }
  }

  private void addToVariablesToFlag(VariableTree variableTree) {
    Type type = variableTree.type().symbolType();
    if (getClasses().stream().anyMatch(type::isSubtypeOf) && isConstructorInitialized(variableTree)) {
      Symbol variableTreeSymbol = variableTree.symbol();
      //Ignore field variables
      if (variableTreeSymbol.isVariableSymbol() && variableTreeSymbol.owner().isMethodSymbol()) {
        VariableSymbol variableSymbol = (VariableSymbol) variableTreeSymbol;
        if (isCorrectlyInitializedViaConstructor(variableTree)) {
          correctlyInitializedViaConstructor.add(variableSymbol);
        }
        else {
          variablesToFlag.add(variableSymbol);
        }
      }
    }
  }

  private static boolean isConstructorInitialized(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    return initializer != null && initializer.is(Tree.Kind.NEW_CLASS);
  }

  // TODO should check the method vs constructor parameters
  private void removeFromVariablesToFlagIfInitializedWithMethod(MethodInvocationTree mit) {
    if (isExpectedMethod(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
      if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
        VariableSymbol reference = (VariableSymbol)((IdentifierTree) mse.expression()).symbol();
        if (methodArgumentsHaveExpectedValue(mit.arguments())) {
          variablesToFlag.remove(reference);
        }
        else if (correctlyInitializedViaConstructor.contains(reference)) {
          variablesToFlag.add(reference);
        }
      }
    }
  }

  private boolean isExpectedMethod(MethodInvocationTree mit) {
    Symbol methodSymbol = mit.symbol();
    boolean hasMethodArity = mit.arguments().size() == getMethodArity();
    if (hasMethodArity && isWantedClassMethod(methodSymbol)) {
      return getMethodName().equals(getIdentifier(mit).name());
    }
    return false;
  }

  private boolean isWantedClassMethod(Symbol methodSymbol) {
    return methodSymbol.isMethodSymbol() && getClasses().stream().anyMatch(methodSymbol.owner().type()::isSubtypeOf);
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
