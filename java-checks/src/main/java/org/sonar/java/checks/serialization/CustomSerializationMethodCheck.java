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
package org.sonar.java.checks.serialization;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2061")
public class CustomSerializationMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (hasSemantic() && isOwnedBySerializable(methodSymbol)) {
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
