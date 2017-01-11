/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.sonar.plugins.java.api.tree.Tree.Kind.CONDITIONAL_AND;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONDITIONAL_OR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONSTRUCTOR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IF_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD;


@Rule(key = "S3776")
public class CognitiveComplexityMethodCheck  extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 15;

  @RuleProperty(
          key = "Threshold",
          description = "The maximum authorized complexity.",
          defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(METHOD, CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    if (method.block() == null || ((ClassTree)method.parent()).simpleName() == null || isWithinLocalClass(method)) {
      return;
    }
    CognitiveComplexityVisitor complexityVisitor = new CognitiveComplexityVisitor();
    method.accept(complexityVisitor);
    int total = complexityVisitor.complexity;
    if (total > max) {
      reportIssue(method.simpleName(),
        "Refactor this method to reduce its Cognitive Complexity from " + total + " to the " + max + " allowed.", complexityVisitor.flow, total - max);
    }

  }

  private boolean isWithinLocalClass(MethodTree method) {
    return hasSemantic() && method.symbol().owner().owner().isMethodSymbol();
  }

  private static class CognitiveComplexityVisitor extends BaseTreeVisitor {

    private final List<JavaFileScannerContext.Location> flow;
    private final Set<Tree> ignored;
    int complexity;
    int nesting;
    boolean ignoreNesting;

    CognitiveComplexityVisitor() {
      complexity = 0;
      nesting = 1;
      ignoreNesting = false;
      flow = new ArrayList<>();
      ignored = new HashSet<>();
    }

    private void increaseComplexityByNesting(Tree tree) {
      increaseComplexity(tree, nesting);
    }

    private void increaseComplexityByOne(Tree tree) {
      increaseComplexity(tree, 1);
    }

    private void increaseComplexity(Tree tree, int increase) {
      complexity += increase;
      if (ignoreNesting) {
        flow.add(new JavaFileScannerContext.Location("+1", tree));
        ignoreNesting = false;
      } else if (!ignored.contains(tree)) {
        String message = "+" + increase;
        if (increase > 1) {
          message += " (incl " + (increase - 1) + " for nesting)";
        }
        flow.add(new JavaFileScannerContext.Location(message, tree));
      }
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      increaseComplexityByNesting(tree.ifKeyword());
      scan(tree.condition());
      nesting++;
      scan(tree.thenStatement());
      nesting--;
      boolean elseStatementNotIF = tree.elseStatement() != null && !tree.elseStatement().is(IF_STATEMENT);
      if(elseStatementNotIF) {
        increaseComplexityByOne(tree.elseKeyword());
        nesting++;
      } else if(tree.elseStatement() != null) {
        // else statement is an if, visiting it will increase complexity by nesting so by one only.
        ignoreNesting = true;
        complexity -= nesting - 1;
      }
      scan(tree.elseStatement());
      if(elseStatementNotIF) {
        nesting--;
      }
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      scan(tree.resources());
      scan(tree.block());
      tree.catches().forEach(c -> increaseComplexityByNesting(c.catchKeyword()));
      nesting++;
      scan(tree.catches());
      nesting--;
      scan(tree.finallyBlock());
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      increaseComplexityByNesting(tree.forKeyword());
      nesting++;
      super.visitForStatement(tree);
      nesting--;
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      increaseComplexityByNesting(tree.forKeyword());
      nesting++;
      super.visitForEachStatement(tree);
      nesting--;
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      increaseComplexityByNesting(tree.whileKeyword());
      nesting++;
      super.visitWhileStatement(tree);
      nesting--;
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      increaseComplexityByNesting(tree.doKeyword());
      nesting++;
      super.visitDoWhileStatement(tree);
      nesting--;
    }

    @Override
    public void visitConditionalExpression(ConditionalExpressionTree tree) {
      increaseComplexityByNesting(tree.questionToken());
      nesting++;
      super.visitConditionalExpression(tree);
      nesting--;
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      increaseComplexityByNesting(tree.switchKeyword());
      nesting++;
      super.visitSwitchStatement(tree);
      nesting--;
    }

    @Override
    public void visitBreakStatement(BreakStatementTree tree) {
      if(tree.label() != null) {
        increaseComplexityByOne(tree.breakKeyword());
      }
      super.visitBreakStatement(tree);
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      if(tree.label() != null) {
        increaseComplexityByOne(tree.continueKeyword());
      }
      super.visitContinueStatement(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      nesting++;
      super.visitClass(tree);
      nesting--;
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      nesting++;
      super.visitLambdaExpression(lambdaExpressionTree);
      nesting--;
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if(tree.is(CONDITIONAL_AND, CONDITIONAL_OR)) {
        increaseComplexityByOne(tree.operatorToken());
        ExpressionTree left = ExpressionUtils.skipParentheses(tree.leftOperand());
        if(left.kind() == tree.kind()) {
          ignored.add(((BinaryExpressionTree) left).operatorToken());
          complexity--;
        }
        ExpressionTree right = ExpressionUtils.skipParentheses(tree.rightOperand());
        if(right.kind() == tree.kind()) {
          ignored.add(((BinaryExpressionTree) right).operatorToken());
          complexity--;
        }
      }
      super.visitBinaryExpression(tree);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

}
