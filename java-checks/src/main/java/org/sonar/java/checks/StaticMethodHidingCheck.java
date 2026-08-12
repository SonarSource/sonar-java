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

@Rule(key = "S9149")
public class StaticMethodHidingCheck extends IssuableSubscriptionVisitor {

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
    if (!methodSymbol.isStatic()) {
      return;
    }
    Symbol.TypeSymbol owner = (Symbol.TypeSymbol) methodSymbol.owner();
    Type superClass = owner.superClass();
    while (superClass != null) {
      if (checkHiding(methodTree, methodSymbol, superClass)) {
        return;
      }
      superClass = superClass.symbol().superClass();
    }
  }

  private boolean checkHiding(MethodTree methodTree, Symbol.MethodSymbol methodSymbol, Type superClass) {
    for (Symbol symbol : superClass.symbol().lookupSymbols(methodSymbol.name())) {
      if (symbol.isMethodSymbol() && symbol.isStatic() && !symbol.isPrivate()
        && hasSameParameterTypes(methodSymbol, (Symbol.MethodSymbol) symbol)) {
        reportHidingIssue(methodTree, methodSymbol, (Symbol.MethodSymbol) symbol);
        return true;
      }
    }
    return false;
  }

  private void reportHidingIssue(MethodTree methodTree, Symbol.MethodSymbol methodSymbol, Symbol.MethodSymbol hiddenMethod) {
    String message = String.format("Rename this method; it hides \"%s\" in \"%s\".",
      methodSymbol.name(), hiddenMethod.owner().name());
    MethodTree declaration = hiddenMethod.declaration();
    if (declaration != null) {
      reportIssue(methodTree.simpleName(), message,
        Collections.singletonList(new JavaFileScannerContext.Location("Hidden method",
          declaration.simpleName())),
        null);
    } else {
      reportIssue(methodTree.simpleName(), message);
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
