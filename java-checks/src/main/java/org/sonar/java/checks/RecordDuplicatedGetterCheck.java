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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6211")
public class RecordDuplicatedGetterCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    Symbol.TypeSymbol recordSymbol = ((ClassTree) tree).symbol();

    for (Symbol.VariableSymbol component : recordComponents(recordSymbol)) {
      findDeclaredMethod(recordSymbol, getterName(component)).ifPresent(m -> checkConflictWithAccessor(recordSymbol, component, m));
    }
  }

  private static String getterName(Symbol.VariableSymbol component) {
    return (component.type().isPrimitive(Primitives.BOOLEAN) ? "is" : "get") + upperCaseFirstCharacter(component.name());
  }

  private void checkConflictWithAccessor(Symbol.TypeSymbol recordSymbol, Symbol.VariableSymbol component, MethodTree getter) {
    if (isDirectCallToAccessor(getter, component) || isPojoGetter(getter, component)) {
      return;
    }
    Optional<MethodTree> accessor = findDeclaredMethod(recordSymbol, component.name());
    if (!accessor.isPresent()) {
      reportIssue(getter.simpleName(), issueMessage(getter, component));
    } else {
      MethodTree accessorMethod = accessor.get();
      if (!SyntacticEquivalence.areEquivalent(accessorMethod.block(), getter.block()) && !isDirectCallToGetter(accessorMethod, getter)) {
        reportIssue(getter.simpleName(), issueMessage(getter, component));
      }
    }
  }

  private static boolean isPojoGetter(MethodTree method, Symbol.VariableSymbol component) {
    return singleReturnStatementExpression(method).filter(expr -> isComponent(expr, component)).isPresent();
  }

  private static boolean isDirectCallToAccessor(MethodTree getter, Symbol.VariableSymbol component) {
    return singleReturnStatementExpression(getter).filter(expr -> isAccessorInvocation(expr, component)).isPresent();
  }

  private static boolean isDirectCallToGetter(MethodTree accessor, MethodTree getter) {
    return singleReturnStatementExpression(accessor).filter(expr -> isGetterInvocation(expr, getter.symbol()))
      .isPresent();
  }

  private static Optional<ExpressionTree> singleReturnStatementExpression(MethodTree method) {
    return Optional.ofNullable(method.block())
      .filter(b -> b.body().size() == 1)
      .map(b -> b.body().get(0))
      .filter(s -> s.is(Tree.Kind.RETURN_STATEMENT))
      .map(ReturnStatementTree.class::cast)
      .map(ReturnStatementTree::expression)
      .filter(Objects::nonNull);
  }

  private static boolean isComponent(ExpressionTree expression, Symbol.VariableSymbol component) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      return component.equals(((IdentifierTree) expression).symbol());
    }
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) expression;
      return ExpressionUtils.isThis(mset.expression())
        && isComponent(mset.identifier(), component);
    }
    return false;
  }

  private static boolean isAccessorInvocation(ExpressionTree expression, Symbol.VariableSymbol component) {
    if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    MethodInvocationTree mit = (MethodInvocationTree) expression;
    Symbol methodSymbol = mit.symbol();
    return mit.arguments().isEmpty()
      && component.name().equals(methodSymbol.name())
      && component.owner().equals(methodSymbol.owner());
  }

  private static boolean isGetterInvocation(ExpressionTree expression, Symbol.MethodSymbol getter) {
    return expression.is(Tree.Kind.METHOD_INVOCATION) && getter.equals(((MethodInvocationTree) expression).symbol());
  }

  private static String issueMessage(MethodTree getter, Symbol.VariableSymbol component) {
    return String.format("Remove this getter '%s()' from record and override an existing one '%s()'.", getter.simpleName().name(), component.name());
  }

  private static List<Symbol.VariableSymbol> recordComponents(Symbol.TypeSymbol recordSymbol) {
    return recordSymbol
      .memberSymbols()
      .stream()
      .filter(Symbol::isVariableSymbol)
      .map(Symbol.VariableSymbol.class::cast)
      .collect(Collectors.toList());
  }

  private static Optional<MethodTree> findDeclaredMethod(Symbol.TypeSymbol recordSymbol, String methodName) {
    return recordSymbol.lookupSymbols(methodName)
      .stream()
      .filter(Symbol::isMethodSymbol)
      .filter(Symbol::isPublic)
      .map(Symbol.MethodSymbol.class::cast)
      .filter(m -> m.parameterTypes().isEmpty())
      // only keep explicit declaration
      .map(MethodSymbol::declaration)
      .filter(Objects::nonNull)
      .findFirst();
  }

  private static String upperCaseFirstCharacter(String string) {
    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }
}
