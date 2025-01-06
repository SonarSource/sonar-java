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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S3551")
public class SynchronizedOverrideCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make this method \"synchronized\" to match the parent class implementation.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    List<MethodSymbol> overriddenSymbols = methodSymbol.overriddenSymbols();
    if (overriddenSymbols.isEmpty()) {
      return;
    }
    Symbol.MethodSymbol overriddenSymbol = overriddenSymbols.get(0);
    if (overriddenSymbol.isSynchronizedMethod() && !methodSymbol.isSynchronizedMethod()) {
      List<JavaFileScannerContext.Location> secondaries = Collections.emptyList();
      MethodTree overriddenMethodTree = overriddenSymbol.declaration();
      if (overriddenMethodTree != null) {
        secondaries = Collections.singletonList(new JavaFileScannerContext.Location("", overriddenMethodTree.simpleName()));
      }
      reportIssue(methodTree.simpleName(), MESSAGE, secondaries, null);
    }
  }

}
