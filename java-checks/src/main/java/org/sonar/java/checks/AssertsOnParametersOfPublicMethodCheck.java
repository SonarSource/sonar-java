/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4274")
public class AssertsOnParametersOfPublicMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  private final Set<AssertStatementTree> assertReported = new HashSet<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    assertReported.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (!methodTree.symbol().isPublic()) {
      return;
    }
    methodTree.parameters().stream()
      .map(VariableTree::symbol)
      .map(Symbol::usages)
      .flatMap(List::stream)
      .forEach(parameter -> checkUsage(parameter, methodTree));
  }

  private void checkUsage(IdentifierTree parameterUsage, MethodTree methodTree) {
    Tree parameterParent = parameterUsage.parent();
    while (!parameterParent.equals(methodTree) && !assertReported.contains(parameterParent)) {
      if (parameterParent.is(Tree.Kind.ASSERT_STATEMENT)) {
        assertReported.add((AssertStatementTree) parameterParent);
        reportIssue(parameterParent, "Replace this assert with a proper check.");
        return;
      }
      parameterParent = parameterParent.parent();
    }
  }
}
