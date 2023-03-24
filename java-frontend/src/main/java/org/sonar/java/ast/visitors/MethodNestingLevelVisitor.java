/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.ast.visitors;

import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

public class MethodNestingLevelVisitor extends BaseTreeVisitor {
  private int maxNestingLevel = 0;
  private int nestingLevel = 0;
  
  public int getMaxNestingLevel(MethodTree tree) {
    maxNestingLevel = 0;
    nestingLevel = 0;
    if(tree.block() == null) return maxNestingLevel;
    scan(tree.block());
    return maxNestingLevel;
  }
  
  void visit(Tree tree) {
    nestingLevel++;
    if(nestingLevel > maxNestingLevel) {
      maxNestingLevel = nestingLevel;
    }
    scan(tree);
    nestingLevel--;
  }
  
  @Override
  public void visitIfStatement(IfStatementTree tree) {
    visit(tree.thenStatement());
    if(tree.elseStatement() != null) {
      visit(tree.elseStatement());
    }
  }
  
  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    for(CaseGroupTree cgt : tree.cases()) {
      visit(cgt);
    }
  }
  
  @Override
  public void visitTryStatement(TryStatementTree tree) {
    visit(tree.block());
    for(CatchTree ct : tree.catches()) {
      visit(ct);
    }
  }
  
  @Override
  public void visitForStatement(ForStatementTree tree) {
    visit(tree.statement());
  }
  
  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    visit(tree.statement());
  }

}
