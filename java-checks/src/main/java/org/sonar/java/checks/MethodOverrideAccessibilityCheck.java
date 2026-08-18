/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9343")
public class MethodOverrideAccessibilityCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (methodSymbol.isStatic()) {
      checkStaticMethodHiding(methodTree, methodSymbol);
    } else {
      checkInstanceMethodOverride(methodTree, methodSymbol);
    }
  }

  private void checkInstanceMethodOverride(MethodTree methodTree, Symbol.MethodSymbol methodSymbol) {
    List<Symbol.MethodSymbol> overriddenSymbols = methodSymbol.overriddenSymbols();
    if (overriddenSymbols.isEmpty()) {
      return;
    }
    Symbol.MethodSymbol overriddenSymbol = overriddenSymbols.get(0);
    if (overriddenSymbol.owner().isInterface()) {
      return;
    }
    int childLevel = accessLevel(methodSymbol);
    int parentLevel = accessLevel(overriddenSymbol);
    if (childLevel > parentLevel) {
      reportAccessibilityIssue(methodTree, overriddenSymbol, parentLevel, childLevel, "overriding");
    }
  }

  private void checkStaticMethodHiding(MethodTree methodTree, Symbol.MethodSymbol methodSymbol) {
    Symbol.TypeSymbol owner = (Symbol.TypeSymbol) methodSymbol.owner();
    Type superClass = owner.superClass();
    while (superClass != null) {
      for (Symbol symbol : superClass.symbol().lookupSymbols(methodSymbol.name())) {
        if (symbol.isMethodSymbol() && symbol.isStatic() && !symbol.isPrivate()
          && hasSameParameterTypes(methodSymbol, (Symbol.MethodSymbol) symbol)) {
          int childLevel = accessLevel(methodSymbol);
          int parentLevel = accessLevel((Symbol.MethodSymbol) symbol);
          if (childLevel > parentLevel) {
            reportAccessibilityIssue(methodTree, (Symbol.MethodSymbol) symbol, parentLevel, childLevel, "hiding");
          }
          return;
        }
      }
      superClass = superClass.symbol().superClass();
    }
  }

  private void reportAccessibilityIssue(MethodTree methodTree, Symbol.MethodSymbol parentMethod,
    int parentLevel, int childLevel, String verb) {
    String message = String.format("Increase of accessibility from \"%s\" to \"%s\" when %s method.",
      accessLevelName(parentLevel), accessLevelName(childLevel), verb);
    MethodTree declaration = parentMethod.declaration();
    if (declaration != null) {
      reportIssue(methodTree.simpleName(), message,
        Collections.singletonList(new JavaFileScannerContext.Location("Parent method", declaration.simpleName())),
        null);
    } else {
      reportIssue(methodTree.simpleName(), message);
    }
  }

  private static int accessLevel(Symbol symbol) {
    if (symbol.isPrivate()) {
      return 0;
    } else if (symbol.isPackageVisibility()) {
      return 1;
    } else if (symbol.isProtected()) {
      return 2;
    } else if (symbol.isPublic()) {
      return 3;
    }
    return -1;
  }

  private static String accessLevelName(int level) {
    switch (level) {
      case 0:
        return "private";
      case 1:
        return "package-private";
      case 2:
        return "protected";
      case 3:
        return "public";
      default:
        return "unknown";
    }
  }

  private static boolean hasSameParameterTypes(Symbol.MethodSymbol method, Symbol.MethodSymbol candidate) {
    List<Type> methodParams = method.parameterTypes();
    List<Type> candidateParams = candidate.parameterTypes();
    if (methodParams.size() != candidateParams.size()) {
      return false;
    }
    for (int i = 0; i < methodParams.size(); i++) {
      Type methodParam = methodParams.get(i);
      Type candidateParam = candidateParams.get(i);
      if (methodParam.isUnknown() || candidateParam.isUnknown()) {
        return false;
      }
      if (!methodParam.erasure().equals(candidateParam.erasure())) {
        return false;
      }
    }
    return true;
  }

}
