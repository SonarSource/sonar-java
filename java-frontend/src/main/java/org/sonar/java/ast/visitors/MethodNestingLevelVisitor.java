/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

/**
 * Used to determine the deepest nesting level of a method, calculated starting from 0 and 
 * increased by 1 for each if, while, for, foreach, lambda, switch, and try block found nested within each other.
 * This was designed to be used as a metric collector for design-oriented rules. 
 */
public class MethodNestingLevelVisitor extends BaseTreeVisitor {
  private int maxNestingLevel = 0;
  private int nestingLevel = 0;

  public int getMaxNestingLevel(MethodTree tree) {
    maxNestingLevel = 0;
    nestingLevel = 0;
    if (tree.block() == null) {
      return maxNestingLevel;
    }
    scan(tree.block());
    return maxNestingLevel;
  }

  void visit(Tree tree) {
    nestingLevel++;
    if (nestingLevel > maxNestingLevel) {
      maxNestingLevel = nestingLevel;
    }
    scan(tree);
    nestingLevel--;
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    visit(tree.thenStatement());
    var elseTree = tree.elseStatement();
    if (elseTree != null) {
      if (elseTree.is(Kind.IF_STATEMENT)) {
        tree.elseStatement().accept(this);
      } else {
        visit(tree.elseStatement());
      }
    }
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    for (CaseGroupTree cgt : tree.cases()) {
      visit(cgt);
    }
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    visit(tree.block());
    for (CatchTree ct : tree.catches()) {
      visit(ct);
    }
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    visit(tree.statement());
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    visit(tree.statement());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    visit(tree.statement());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    visit(lambdaExpressionTree.body());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    visit(tree.statement());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.classBody() != null) {
      visit(tree.classBody());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.block() != null) {
      visit(tree.block());
    }
  }

}
