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
package org.sonar.java.ast.visitors;

import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatementVisitor extends BaseTreeVisitor {
  private int statements;
  private Set<Tree> variableTypes = new HashSet<>();

  public int numberOfStatements(Tree tree) {
    statements = 0;
    variableTypes.clear();
    scan(tree);
    statements += variableTypes.size();
    return statements;
  }

  @Override
  public void visitEmptyStatement(EmptyStatementTree tree) {
    statements++;
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    statements++;
    super.visitIfStatement(tree);
  }

  @Override
  public void visitAssertStatement(AssertStatementTree tree) {

    statements++;
    super.visitAssertStatement(tree);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    statements++;
    super.visitSwitchStatement(tree);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    statements++;
    super.visitWhileStatement(tree);
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    statements++;
    super.visitDoWhileStatement(tree);
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    statements++;
    super.visitForStatement(tree);
    removeVariable(tree.initializer());
    removeVariable(tree.update());
  }

  private void removeVariable(List<StatementTree> statementTrees) {
    for (StatementTree statementTree : statementTrees) {
      if (statementTree.is(Tree.Kind.VARIABLE)) {
        variableTypes.remove(((VariableTree) statementTree).type());
      } else {
        statements--;
      }
    }
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    statements++;
    super.visitForEachStatement(tree);
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    statements++;
    super.visitBreakStatement(tree);
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    statements++;
    super.visitContinueStatement(tree);
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    statements++;
    super.visitReturnStatement(tree);
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    statements++;
    super.visitThrowStatement(tree);
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    statements++;
    super.visitSynchronizedStatement(tree);
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    statements++;
    statements -= tree.resourceList().size();
    statements -= tree.catches().size();
    super.visitTryStatement(tree);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    variableTypes.add(tree.type());
    super.visitVariable(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);
    for (VariableTree variableTree : tree.parameters()) {
      variableTypes.remove(variableTree.type());
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    super.visitClass(tree);
    for (Tree member : tree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        variableTypes.remove(((VariableTree) member).type());
      }
    }
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    statements++;
    super.visitExpressionStatement(tree);
  }

}
