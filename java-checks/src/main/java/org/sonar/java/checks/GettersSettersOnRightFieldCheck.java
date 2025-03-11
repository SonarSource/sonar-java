/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
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
    MethodTree methodTree = (MethodTree) tree;
    MethodTreeUtils.hasGetterSignature(methodTree.symbol()).ifPresent(fieldName -> checkGetter(fieldName, methodTree));
    MethodTreeUtils.hasSetterSignature(methodTree.symbol()).ifPresent(fieldName -> checkSetter(fieldName, methodTree));
  }

  private void checkGetter(String fieldName, MethodTree methodTree) {
    Symbol.TypeSymbol getterOwner = ((Symbol.TypeSymbol) methodTree.symbol().owner());
    if (hasNoPrivateFieldMatchingNameAndType(fieldName, methodTree.symbol().returnType().type(), getterOwner)) {
      return;
    }
    MethodTreeUtils.hasGetterBody(methodTree)
      .filter(returnSymbol -> !fieldName.equals(returnSymbol.name()))
      .ifPresent(returnedSymbol -> context.reportIssue(this, methodTree.simpleName(),
        "Refactor this getter so that it actually refers to the field \"" + fieldName + "\"."));
  }

  private void checkSetter(String fieldName, MethodTree methodTree) {
    Symbol.TypeSymbol setterOwner = ((Symbol.TypeSymbol) methodTree.symbol().owner());
    if (hasNoPrivateFieldMatchingNameAndType(fieldName, methodTree.symbol().parameterTypes().get(0), setterOwner)) {
      return;
    }
    MethodTreeUtils.hasSetterBody(methodTree)
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



}
