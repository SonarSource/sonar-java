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

import java.util.ArrayList;
import java.util.List;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

public class ComplexityVisitor extends BaseTreeVisitor {

  private List<Tree> blame = new ArrayList<>();
  private Tree root;
  private static final String DEFAULT_KEYWORD = JavaKeyword.DEFAULT.getValue();

  public List<Tree> getNodes(Tree tree) {
    blame.clear();
    root = tree;
    scan(tree);
    root = null;
    return blame;
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.block() != null) {
      blame.add(tree.simpleName().identifierToken());
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    if(root.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.COMPILATION_UNIT)) {
      super.visitClass(tree);
    }
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    if(root.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.COMPILATION_UNIT) || lambdaExpressionTree.equals(root)) {
      blame.add(lambdaExpressionTree.arrowToken());
      super.visitLambdaExpression(lambdaExpressionTree);
    }
  }

  @Override
  public void visitCaseLabel(CaseLabelTree tree) {
    if (!DEFAULT_KEYWORD.equals(tree.caseOrDefaultKeyword().text())) {
      // default keyword does not count in complexity
      blame.add(tree.firstToken());
    }
    super.visitCaseLabel(tree);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    blame.add(tree.firstToken());
    super.visitForEachStatement(tree);
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    blame.add(tree.firstToken());
    super.visitForStatement(tree);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    blame.add(tree.firstToken());
    super.visitWhileStatement(tree);
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    blame.add(tree.firstToken());
    super.visitDoWhileStatement(tree);
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    blame.add(tree.firstToken());
    super.visitIfStatement(tree);
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    blame.add(tree.questionToken());
    super.visitConditionalExpression(tree);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (tree.is(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR)) {
      blame.add(tree.operatorToken());
    }
    super.visitBinaryExpression(tree);
  }
}
