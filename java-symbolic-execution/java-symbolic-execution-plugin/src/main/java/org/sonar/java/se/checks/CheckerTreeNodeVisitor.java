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
package org.sonar.java.se.checks;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.se.ProgramState;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class CheckerTreeNodeVisitor extends BaseTreeVisitor {

  protected ProgramState programState;

  protected CheckerTreeNodeVisitor(ProgramState programState) {
    this.programState = programState;
  }

  @Override
  protected void scan(@Nullable Tree tree) {
    // Cut recursive processing
  }

  @Override
  protected void scan(List<? extends Tree> trees) {
    // Cut recursive processing
  }

  @Override
  protected void scan(@Nullable ListTree<? extends Tree> listTree) {
    // Cut recursive processing
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    // as this specific call does not use scan : cut recursive processing
  }
}

