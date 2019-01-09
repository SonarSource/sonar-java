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
package org.sonar.java.se.checks;

import org.sonar.java.se.ProgramState;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public abstract class CheckerTreeNodeVisitor extends BaseTreeVisitor {

  protected ProgramState programState;

  protected CheckerTreeNodeVisitor(ProgramState programState) {
    this.programState = programState;
  }

  @Override
  protected void scan(Tree tree) {
    // Cut recursive processing
  }

  @Override
  protected void scan(List<? extends Tree> trees) {
    // Cut recursive processing
  }

  @Override
  protected void scan(ListTree<? extends Tree> listTree) {
    // Cut recursive processing
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    // as this specific call does not use scan : cut recursive processing
  }
}

