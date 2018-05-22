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
import org.sonar.java.model.LiteralUtils;
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
  private final List<VariableSymbol> declarationsToFlag = Lists.newArrayList();
  private final List<MethodInvocationTree> settersToFlag = Lists.newArrayList();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  protected abstract String getMessage();

  protected abstract boolean constructorInitializesCorrectly(VariableTree variableSymbol);

  protected abstract String getSetterName();

  protected abstract List<String> getClasses();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    correctlyInitializedViaConstructor.clear();
    declarationsToFlag.clear();
    settersToFlag.clear();
    super.scanFile(context);
    for (VariableSymbol var : declarationsToFlag) {
      if (var.declaration() != null) {
        reportIssue(var.declaration().simpleName(), getMessage());
      }
    }
    for (MethodInvocationTree mit : settersToFlag) {
      reportIssue(mit.arguments(), getMessage());
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
        checkSetterInvocation(mit);
      }
    }
  }

  private void addToVariablesToFlag(VariableTree variableTree) {
    Type type = variableTree.type().symbolType();
    if (getClasses().stream().anyMatch(type::isSubtypeOf) && isInitializedByConstructor(variableTree)) {
      Symbol variableTreeSymbol = variableTree.symbol();
      //Ignore field variables
      if (variableTreeSymbol.isVariableSymbol() && variableTreeSymbol.owner().isMethodSymbol()) {
        VariableSymbol variableSymbol = (VariableSymbol) variableTreeSymbol;
        if (constructorInitializesCorrectly(variableTree)) {
          correctlyInitializedViaConstructor.add(variableSymbol);
        } else {
          declarationsToFlag.add(variableSymbol);
        }
      }
    }
  }

  private static boolean isInitializedByConstructor(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    return initializer != null && initializer.is(Tree.Kind.NEW_CLASS);
  }

  private void checkSetterInvocation(MethodInvocationTree mit) {
    if (isExpectedSetter(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
      if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
        VariableSymbol reference = (VariableSymbol)((IdentifierTree) mse.expression()).symbol();
        if (setterArgumentHasExpectedValue(mit.arguments())) {
          declarationsToFlag.remove(reference);
        } else if (correctlyInitializedViaConstructor.contains(reference)) {
          declarationsToFlag.add(reference);
        } else if (!declarationsToFlag.contains(reference)) {
          settersToFlag.add(mit);
        }
      }
    }
  }

  private boolean isExpectedSetter(MethodInvocationTree mit) {
    Symbol methodSymbol = mit.symbol();
    boolean hasMethodArity = mit.arguments().size() == 1;
    if (hasMethodArity && isWantedClassMethod(methodSymbol)) {
      return getSetterName().equals(getIdentifier(mit).name());
    }
    return false;
  }

  private boolean setterArgumentHasExpectedValue(Arguments arguments) {
    ExpressionTree expressionTree = arguments.get(0);
    return !LiteralUtils.isFalse(expressionTree);
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
