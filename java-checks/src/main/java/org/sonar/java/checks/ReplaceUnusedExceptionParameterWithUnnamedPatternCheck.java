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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S7467")
public class ReplaceUnusedExceptionParameterWithUnnamedPatternCheck extends IssuableSubscriptionVisitor {
  private static final String UNNAMED_PATTERN = "_";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    CatchTree catchTree = (CatchTree) tree;
    VariableTree v = catchTree.parameter();
    String name = v.simpleName().name();
    if (!UNNAMED_PATTERN.equals(name)
      && v.symbol().usages().isEmpty()) {
      reportIssue(v.simpleName(), String.format("Replace %s with an unnamed pattern.", name));
    }
  }

}
