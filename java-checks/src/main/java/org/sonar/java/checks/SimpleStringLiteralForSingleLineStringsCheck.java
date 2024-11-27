/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5663")
public class SimpleStringLiteralForSingleLineStringsCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "Use simple literal for a single-line string.";
  
  @Override
  public List<Tree.Kind> nodesToVisit() { 
    return Collections.singletonList(Tree.Kind.TEXT_BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree textBlock = (LiteralTree) tree;
    String value = textBlock.value();

    if (value.split("(\r?\n)|\r").length <= 2) {
      reportIssue(tree, MESSAGE);
    }
  }
}
