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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
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
    overriddenSymbols.stream()
      .filter(s -> !s.owner().isInterface())
      .findFirst()
      .ifPresent(overriddenSymbol -> reportIfAccessIncreased(methodTree, methodSymbol, overriddenSymbol, "overriding"));
  }

  private void checkStaticMethodHiding(MethodTree methodTree, Symbol.MethodSymbol methodSymbol) {
    MethodTreeUtils.findHiddenStaticMethod(methodSymbol,
      symbol -> !(symbol.isPackageVisibility() && !samePackage(methodSymbol, symbol)))
      .ifPresent(hidden -> reportIfAccessIncreased(methodTree, methodSymbol, hidden, "hiding"));
  }

  private static boolean samePackage(Symbol s1, Symbol s2) {
    return JUtils.getPackage(s1).equals(JUtils.getPackage(s2));
  }

  private void reportIfAccessIncreased(MethodTree methodTree, Symbol childMethod, Symbol.MethodSymbol parentMethod, String verb) {
    int childLevel = accessLevel(childMethod);
    int parentLevel = accessLevel(parentMethod);
    if (parentLevel < 0 || childLevel < 0 || childLevel <= parentLevel) {
      return;
    }
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
    if (symbol.isPrivate()) return 0;
    if (symbol.isPackageVisibility()) return 1;
    if (symbol.isProtected()) return 2;
    if (symbol.isPublic()) return 3;
    return -1;
  }

  private static String accessLevelName(int level) {
    return switch (level) {
      case 0 -> "private";
      case 1 -> "package-private";
      case 2 -> "protected";
      case 3 -> "public";
      default -> "unknown";
    };
  }

}
