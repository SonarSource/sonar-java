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
package org.sonar.java.checks.serialization;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2061")
public class CustomSerializationMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (isOwnedBySerializable(methodSymbol)) {
      if (hasSignature(methodSymbol, "writeObject", "java.io.ObjectOutputStream")
        || hasSignature(methodSymbol, "readObject", "java.io.ObjectInputStream")
        || hasSignature(methodSymbol, "readObjectNoData")) {
        checkPrivate(methodTree);
        checkNotStatic(methodTree);
      } else if (hasSignature(methodSymbol, "writeReplace")
        || hasSignature(methodSymbol, "readResolve")) {
        checkNotStatic(methodTree);
        checkReturnType(methodTree, "java.lang.Object");
      }
    }
  }

  private static boolean isOwnedBySerializable(Symbol.MethodSymbol methodSymbol) {
    Symbol.TypeSymbol owner = (Symbol.TypeSymbol) methodSymbol.owner();
    return owner.type().isSubtypeOf("java.io.Serializable");
  }

  private static boolean hasSignature(Symbol.MethodSymbol methodSymbol, String name, String paramType) {
    return name.equals(methodSymbol.name()) && hasSingleParam(methodSymbol, paramType);
  }

  private static boolean hasSignature(Symbol.MethodSymbol methodSymbol, String name) {
    return name.equals(methodSymbol.name()) && methodSymbol.parameterTypes().isEmpty();
  }

  private static boolean hasSingleParam(Symbol.MethodSymbol methodSymbol, String searchedParamType) {
    List<Type> parametersTypes = methodSymbol.parameterTypes();
    return parametersTypes.size() == 1 && parametersTypes.get(0).is(searchedParamType);
  }

  private void checkNotStatic(MethodTree methodTree) {
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (methodSymbol.isStatic()) {
      reportIssue(methodTree.simpleName(), "The \"static\" modifier should not be applied to \"" + methodSymbol.name() + "\".");
    }
  }

  private void checkPrivate(MethodTree methodTree) {
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (!methodSymbol.isPrivate()) {
      reportIssue(methodTree.simpleName(), "Make \"" + methodSymbol.name() + "\" \"private\".");
    }
  }

  private void checkReturnType(MethodTree methodTree, String requiredReturnType) {
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (!methodSymbol.returnType().type().is(requiredReturnType)) {
      reportIssue(methodTree.simpleName(), "\"" + methodSymbol.name() + "\" should return \"" + requiredReturnType + "\".");
    }
  }

}
