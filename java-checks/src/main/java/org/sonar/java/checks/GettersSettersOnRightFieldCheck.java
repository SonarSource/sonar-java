/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4275")
public class GettersSettersOnRightFieldCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    isGetterLike(methodTree.symbol()).ifPresent(fieldName -> checkGetter(fieldName, methodTree));
    isSetterLike(methodTree.symbol()).ifPresent(fieldName -> checkSetter(fieldName, methodTree));
  }

  private static Optional<String> isGetterLike(Symbol.MethodSymbol methodSymbol) {
    if (!methodSymbol.parameterTypes().isEmpty() || isPrivateStaticOrAbstract(methodSymbol)) {
      return Optional.empty();
    }
    String methodName = methodSymbol.name();
    if (methodName.length() > 3 && methodName.startsWith("get")) {
      return Optional.of(lowerCaseFirstLetter(methodName.substring(3)));
    }
    if (methodName.length() > 2 && methodName.startsWith("is")) {
      return Optional.of(lowerCaseFirstLetter(methodName.substring(2)));
    }
    return Optional.empty();
  }

  private static Optional<String> isSetterLike(Symbol.MethodSymbol methodSymbol) {
    if (methodSymbol.parameterTypes().size() != 1 || isPrivateStaticOrAbstract(methodSymbol)) {
      return Optional.empty();
    }
    String methodName = methodSymbol.name();
    if (methodName.length() > 3 && methodName.startsWith("set") && methodSymbol.returnType().type().isVoid()) {
      return Optional.of(lowerCaseFirstLetter(methodName.substring(3)));
    }
    return Optional.empty();
  }

  private static boolean isPrivateStaticOrAbstract(Symbol.MethodSymbol methodSymbol) {
    return methodSymbol.isPrivate() || methodSymbol.isStatic() || methodSymbol.isAbstract();
  }

  private static String lowerCaseFirstLetter(String methodName) {
    return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
  }

  private void checkGetter(String fieldName, MethodTree methodTree) {
    Symbol.TypeSymbol getterOwner = ((Symbol.TypeSymbol) methodTree.symbol().owner());
    if (hasNoPrivateFieldMatchingNameAndType(fieldName, methodTree.symbol().returnType().type(), getterOwner)) {
      return;
    }
    firstAndOnlyStatement(methodTree)
      .filter(statementTree -> statementTree.is(Tree.Kind.RETURN_STATEMENT))
      .map(statementTree -> ((ReturnStatementTree) statementTree).expression())
      .flatMap(GettersSettersOnRightFieldCheck::symbolFromExpression)
      .filter(returnSymbol -> !fieldName.equals(returnSymbol.name()))
      .ifPresent(returnedSymbol -> context.reportIssue(this, methodTree.simpleName(),
        "Refactor this getter so that it actually refers to the field \"" + fieldName + "\"."));
  }

  private void checkSetter(String fieldName, MethodTree methodTree) {
    Symbol.TypeSymbol setterOwner = ((Symbol.TypeSymbol) methodTree.symbol().owner());
    if (hasNoPrivateFieldMatchingNameAndType(fieldName, methodTree.symbol().parameterTypes().get(0), setterOwner)) {
      return;
    }
    firstAndOnlyStatement(methodTree)
      .filter(statementTree -> statementTree.is(Tree.Kind.EXPRESSION_STATEMENT))
      .map(statementTree -> ((ExpressionStatementTree) statementTree).expression())
      .filter(expressionTree -> expressionTree.is(Tree.Kind.ASSIGNMENT))
      .map(expressionTree -> ((AssignmentExpressionTree) expressionTree).variable())
      .flatMap(GettersSettersOnRightFieldCheck::symbolFromExpression)
      .filter(variableSymbol -> !fieldName.equals(variableSymbol.name()))
      .ifPresent(variableSymbol -> context.reportIssue(this, methodTree.simpleName(),
        "Refactor this setter so that it actually refers to the field \"" + fieldName + "\"."));
  }

  private static boolean hasNoPrivateFieldMatchingNameAndType(String fieldName, Type fieldType, Symbol.TypeSymbol accessorOwner) {
    return accessorOwner.lookupSymbols(fieldName).stream()
      .filter(Symbol::isVariableSymbol)
      .filter(Symbol::isPrivate)
      .filter(symbol -> symbol.type().equals(fieldType) || symbol.type().isSubtypeOf(fieldType))
      .noneMatch(symbol -> fieldName.equals(symbol.name()));
  }

  private static Optional<Symbol> symbolFromExpression(ExpressionTree returnExpression) {
    if (returnExpression.is(Tree.Kind.IDENTIFIER)) {
      return Optional.of(((IdentifierTree) returnExpression).symbol());
    }
    if (returnExpression.is(Tree.Kind.MEMBER_SELECT)) {
      return Optional.of(((MemberSelectExpressionTree) returnExpression).identifier().symbol());
    }
    return Optional.empty();
  }

  private static Optional<StatementTree> firstAndOnlyStatement(MethodTree methodTree) {
    return Optional.ofNullable(methodTree.block())
      .filter(b -> b.body().size() == 1)
      .map(b -> b.body().get(0));
  }
}
