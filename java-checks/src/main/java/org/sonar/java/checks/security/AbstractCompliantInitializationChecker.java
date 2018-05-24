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
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class AbstractCompliantInitializationChecker extends IssuableSubscriptionVisitor {

  private final List<VariableSymbol> compliantConstructorInitializations = Lists.newArrayList();
  private final List<VariableSymbol> declarationsToReport = Lists.newArrayList();
  private final List<MethodInvocationTree> settersToReport = Lists.newArrayList();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  protected abstract String getMessage();

  protected abstract boolean isCompliantConstructorCall(NewClassTree newClassTree);

  protected abstract List<String> getSetterNames();

  protected abstract List<String> getClasses();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    compliantConstructorInitializations.clear();
    declarationsToReport.clear();
    settersToReport.clear();
    super.scanFile(context);
    for (VariableSymbol var : declarationsToReport) {
      VariableTree declaration = var.declaration();
      if (declaration != null) {
        reportIssue(declaration.simpleName(), getMessage());
      }
    }
    for (MethodInvocationTree mit : settersToReport) {
      reportIssue(mit.arguments(), getMessage());
    }
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        categorizeBasedOnInitialization((VariableTree) tree);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        checkSetterInvocation((MethodInvocationTree) tree);
      }
    }
  }

  private void categorizeBasedOnInitialization(VariableTree variableTree) {
    if (isInitializedByConstructor(variableTree) && variableOrConstructorAreSupportedTypes(variableTree)) {
      Symbol variableTreeSymbol = variableTree.symbol();
      //Ignore field variables
      if (variableTreeSymbol.isVariableSymbol() && variableTreeSymbol.owner().isMethodSymbol()) {
        VariableSymbol variableSymbol = (VariableSymbol) variableTreeSymbol;
        if (isCompliantConstructorCall((NewClassTree) variableTree.initializer())) {
          compliantConstructorInitializations.add(variableSymbol);
        } else {
          declarationsToReport.add(variableSymbol);
        }
      }
    }
  }

  private static boolean isInitializedByConstructor(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    return initializer != null && initializer.is(Tree.Kind.NEW_CLASS);
  }

  private boolean variableOrConstructorAreSupportedTypes(VariableTree variableTree) {
    Type variableType = variableTree.type().symbolType();
    Type newClassType = variableTree.initializer().symbolType();
    return getClasses().stream().anyMatch(variableType::isSubtypeOf)
        || getClasses().stream().anyMatch(newClassType::isSubtypeOf);
  }

  private void checkSetterInvocation(MethodInvocationTree mit) {
    if (isExpectedSetter(mit)) {
      if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        if (((MemberSelectExpressionTree) mit.methodSelect()).expression().is(Tree.Kind.IDENTIFIER)) {
          treatMethodCallOnVariable(mit);
        } else if (!setterArgumentHasCompliantValue(mit.arguments())) {
          // builder method
          settersToReport.add(mit);
        }
      } else if (mit.methodSelect().is(Tree.Kind.IDENTIFIER) && !setterArgumentHasCompliantValue(mit.arguments())) {
        // sub-class method
        settersToReport.add(mit);
      }
    }
  }

  private boolean isExpectedSetter(MethodInvocationTree mit) {
    Symbol methodSymbol = mit.symbol();
    boolean hasMethodArity = mit.arguments().size() == 1;
    if (hasMethodArity && isWantedClassMethod(methodSymbol)) {
      return getSetterNames().contains(getIdentifier(mit).name());
    }
    return false;
  }

  private void treatMethodCallOnVariable(MethodInvocationTree mit) {
    MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
    VariableSymbol reference = (VariableSymbol) ((IdentifierTree) mse.expression()).symbol();
    if (setterArgumentHasCompliantValue(mit.arguments())) {
      declarationsToReport.remove(reference);
    } else if (compliantConstructorInitializations.contains(reference)) {
      declarationsToReport.add(reference);
    } else if (!declarationsToReport.contains(reference)) {
      settersToReport.add(mit);
    }
  }

  private static boolean setterArgumentHasCompliantValue(Arguments arguments) {
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
