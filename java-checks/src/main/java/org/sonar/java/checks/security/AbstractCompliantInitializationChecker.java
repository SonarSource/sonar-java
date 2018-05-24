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
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class AbstractCompliantInitializationChecker extends IssuableSubscriptionVisitor {

  private final List<VariableSymbol> compliantConstructorInitializations = Lists.newArrayList();
  private final List<VariableSymbol> variablesToReport = Lists.newArrayList();
  private final List<MethodInvocationTree> settersToReport = Lists.newArrayList();
  private final List<NewClassTree> newClassToReport = Lists.newArrayList();

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(
      Kind.VARIABLE,
      Kind.ASSIGNMENT,
      Kind.METHOD_INVOCATION,
      Kind.RETURN_STATEMENT);
  }

  protected abstract String getMessage();

  protected abstract boolean isCompliantConstructorCall(NewClassTree newClassTree);

  protected abstract List<String> getSetterNames();

  protected abstract List<String> getClasses();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    compliantConstructorInitializations.clear();
    variablesToReport.clear();
    settersToReport.clear();
    super.scanFile(context);
    for (VariableSymbol var : variablesToReport) {
      VariableTree declaration = var.declaration();
      if (declaration != null) {
        reportIssue(declaration.simpleName(), getMessage());
      }
    }
    for (MethodInvocationTree mit : settersToReport) {
      reportIssue(mit.arguments(), getMessage());
    }
    for (NewClassTree newClassTree : newClassToReport) {
      reportIssue(newClassTree, getMessage());
    }
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Kind.VARIABLE)) {
        categorizeBasedOnConstructor((VariableTree) tree);
      } else if (tree.is(Kind.ASSIGNMENT)) {
        categorizeBasedOnConstructor((AssignmentExpressionTree) tree);
      } else if (tree.is(Kind.METHOD_INVOCATION)) {
        checkSetterInvocation((MethodInvocationTree) tree);
      } else if (tree.is(Kind.RETURN_STATEMENT)) {
        categorizeBasedOnConstructor((ReturnStatementTree) tree);
      }
    }
  }

  private void categorizeBasedOnConstructor(VariableTree declaration) {
    if (isSupported(declaration)) {
      categorizeBasedOnConstructor((NewClassTree) declaration.initializer(),
        (VariableSymbol) declaration.symbol());
    }
  }

  private void categorizeBasedOnConstructor(AssignmentExpressionTree assignment) {
    if (isSupported(assignment)) {
      categorizeBasedOnConstructor((NewClassTree) assignment.expression(),
        (VariableSymbol) ((IdentifierTree) assignment.variable()).symbol());
    }
  }

  private void categorizeBasedOnConstructor(ReturnStatementTree returnStatement) {
    ExpressionTree returnedExpression = returnStatement.expression();
    if (returnedExpression != null && returnedExpression.is(Kind.NEW_CLASS)) {
      NewClassTree newClass = (NewClassTree) returnStatement.expression();
      if (!isCompliantConstructorCall(newClass) && getClasses().stream().anyMatch(newClass.symbolType()::isSubtypeOf)) {
        newClassToReport.add(newClass);
      }
    }
  }

  private void categorizeBasedOnConstructor(NewClassTree newClassTree, VariableSymbol variableSymbol) {
    if (isCompliantConstructorCall(newClassTree)) {
      compliantConstructorInitializations.add(variableSymbol);
    } else {
      variablesToReport.add(variableSymbol);
    }
  }

  private boolean isSupported(VariableTree declaration) {
    ExpressionTree initializer = declaration.initializer();
    if (initializer != null && initializer.is(Kind.NEW_CLASS)) {
      Symbol variableTreeSymbol = declaration.symbol();
      boolean isMethodVariable = variableTreeSymbol.isVariableSymbol() && variableTreeSymbol.owner().isMethodSymbol();
      boolean isSupportedClass = getClasses().stream().anyMatch(declaration.type().symbolType()::isSubtypeOf)
          || getClasses().stream().anyMatch(initializer.symbolType()::isSubtypeOf);
      return isMethodVariable && isSupportedClass;
    }
    return false;
  }

  private boolean isSupported(AssignmentExpressionTree assignment) {
    if (assignment.expression().is(Kind.NEW_CLASS) && assignment.variable().is(Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) assignment.variable();
      boolean isMethodVariable = identifier.symbol().isVariableSymbol()
        && identifier.symbol().owner().isMethodSymbol();
      boolean isSupportedClass = getClasses().stream().anyMatch(identifier.symbolType()::isSubtypeOf)
        || getClasses().stream().anyMatch(assignment.expression().symbolType()::isSubtypeOf);
      return isMethodVariable && isSupportedClass;
    }
    return false;
  }

  private void checkSetterInvocation(MethodInvocationTree mit) {
    if (isExpectedSetter(mit)) {
      if (mit.methodSelect().is(Kind.MEMBER_SELECT)) {
        boolean isCalledOnIdentifier = ((MemberSelectExpressionTree) mit.methodSelect()).expression().is(Kind.IDENTIFIER);
        if (isCalledOnIdentifier) {
          updateIssuesToReport(mit);
        } else if (!setterArgumentHasCompliantValue(mit.arguments())) {
          // builder method
          settersToReport.add(mit);
        }
      } else if (mit.methodSelect().is(Kind.IDENTIFIER) && !setterArgumentHasCompliantValue(mit.arguments())) {
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

  private void updateIssuesToReport(MethodInvocationTree mit) {
    MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
    VariableSymbol reference = (VariableSymbol) ((IdentifierTree) mse.expression()).symbol();
    if (setterArgumentHasCompliantValue(mit.arguments())) {
      variablesToReport.remove(reference);
    } else if (compliantConstructorInitializations.contains(reference)) {
      variablesToReport.add(reference);
    } else if (!variablesToReport.contains(reference)) {
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
    if (mit.methodSelect().is(Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
