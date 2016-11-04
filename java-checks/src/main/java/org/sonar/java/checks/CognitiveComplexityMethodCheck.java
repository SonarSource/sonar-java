/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.ArrayList;
import java.util.List;

import static org.sonar.plugins.java.api.tree.Tree.Kind.*;


@Rule(key = "S3776")
public class CognitiveComplexityMethodCheck  extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 10;

  @RuleProperty(
          key = "Threshold",
          description = "The maximum authorized complexity.",
          defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;


  private List<Tree.Kind> kindsAffectedByNesting = ImmutableList.<Tree.Kind>builder()
          .add(IF_STATEMENT)
          .add(CONDITIONAL_EXPRESSION)  // ternary
          .add(FOR_STATEMENT)
          .add(FOR_EACH_STATEMENT)
          .add(DO_STATEMENT)
          .add(WHILE_STATEMENT)
          .add(SWITCH_STATEMENT)
          .build();


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(METHOD, CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {

    MethodTree method = (MethodTree) tree;
    if (method.block() == null || ((ClassTree)method.parent()).simpleName() == null) {
      return;
    }

    List<JavaFileScannerContext.Location> flow = new ArrayList<>();
    int total = countComplexity(method.block().body(), flow, 0);

    if (total > max) {
      reportIssue(
              method.simpleName(),
              "Refactor this method to reduce its Cognitive Complexity from " + total + " to the " + max + " allowed.",
              flow,
              total - max);
    }

  }

  private int countComplexity(List<StatementTree> statements, List<JavaFileScannerContext.Location> flow, int nestingLevel) {
    if (statements.isEmpty()) {
      return 0;
    }

    int total = 0;

    for (StatementTree st : statements) {

      Tree tree = getSegmentOfInterest(st);

      if (kindsAffectedByNesting.contains(tree.kind())) {
        int hit = 1 + nestingLevel;

        addSecondaryLocation(flow, tree, hit, nestingLevel);

        total += hit;
        total += countExtraConditions(tree, flow);
        total += countIfElseChains(tree, flow, nestingLevel);

      } else {
        total += countBreakAndContinue(tree, flow);
        total += countTryChains(tree, flow, nestingLevel);
        total += countAnonymousClasses(tree, flow, nestingLevel);
      }

      if (tree.is(TRY_STATEMENT)) {
        total += countComplexity(getChildren(tree), flow, nestingLevel);
      } else {
        total += countComplexity(getChildren(tree), flow, nestingLevel + 1);
      }
    }

    return total;
  }

  private void addSecondaryLocation(List<JavaFileScannerContext.Location> flow, Tree st, int hit,  int nestingLevel) {

    Tree tree = st;

    switch (st.kind()) {
      case IF_STATEMENT:
        tree = ((IfStatementTree) st).ifKeyword();
        break;
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        tree = ((BinaryExpressionTree) st).operatorToken();
        break;
      case SWITCH_STATEMENT:
        tree = ((SwitchStatementTree) st).switchKeyword();
        break;
      case WHILE_STATEMENT:
        tree = ((WhileStatementTree) st).whileKeyword();
        break;
      case DO_STATEMENT:
        tree = ((DoWhileStatementTree) st).doKeyword();
        break;
      case FOR_EACH_STATEMENT:
        tree = ((ForEachStatement) st).forKeyword();
        break;
      case FOR_STATEMENT:
        tree = ((ForStatementTree) st).forKeyword();
        break;
      case LAMBDA_EXPRESSION:
        tree = ((LambdaExpressionTree) st).arrowToken();
        break;
      case CATCH:
        tree = ((CatchTree) st).catchKeyword();
        break;
      case BLOCK:
        tree = ((BlockTree) st).openBraceToken();
        if (st.parent().is(IF_STATEMENT)) {
          tree = ((IfStatementTree) st.parent()).elseKeyword();
        }
        break;
      default:
        break;
    }

    if (nestingLevel > 0) {
      flow.add(new JavaFileScannerContext.Location("+" + hit +" (incl " + nestingLevel + " for nesting)", tree));
    } else {
      flow.add(new JavaFileScannerContext.Location("+" + hit, tree));
    }
  }


  private int countAnonymousClasses(Tree st, List<JavaFileScannerContext.Location> flow, int nestingLevel) {
    int total = 0;

    if (st.is(Tree.Kind.CLASS) && ! ((ClassTree)st).members().isEmpty()) {

      for (Tree member : ((ClassTree) st).members()) {
        if (member.is(Tree.Kind.METHOD) && ((MethodTree) member).block().body() != null) {
          total += countComplexity(((MethodTree) member).block().body(), flow, nestingLevel+1);
        }
      }
    }

    return total;
  }

  private int countIfElseChains(Tree st, List<JavaFileScannerContext.Location> flow, int nestingLevel) {
    int total = 0;

    if (! st.is(Tree.Kind.IF_STATEMENT)) {
      return total;
    }

    if (((IfStatementTree) st).elseStatement() == null) {
      return total;
    }

    StatementTree elseStatement = ((IfStatementTree) st).elseStatement();

    total++;

    if (elseStatement.is(Tree.Kind.IF_STATEMENT)) {
      addSecondaryLocation(flow, elseStatement, total, nestingLevel);

      total += countConditions(((IfStatementTree) elseStatement).condition(), flow);
      total += countIfElseChains(elseStatement, flow, nestingLevel);
    } else {
      // `else`
      total += nestingLevel;
      addSecondaryLocation(flow, elseStatement, total, nestingLevel);
    }

    List<StatementTree> children = getChildren(elseStatement);
    total += countComplexity(children, flow, nestingLevel+1);

    return total;
  }

  private int countExtraConditions(Tree tree, List<JavaFileScannerContext.Location> flow) {
    switch (tree.kind()) {
      case WHILE_STATEMENT:
        return countConditions(((WhileStatementTree) tree).condition(), flow);
      case FOR_STATEMENT:
        return countConditions(((ForStatementTree) tree).condition(), flow);
      case DO_STATEMENT:
        return countConditions(((DoWhileStatementTree) tree).condition(), flow);
      case IF_STATEMENT:
        return countConditions(((IfStatementTree) tree).condition(), flow);
      default:
        return 0;
    }
  }

  /**
   * Increment for each non-like operator. So:
   *   a           // +0
   *   a && b      // +1
   *   a && b && c // +1
   *   a && b || c // +2
   *
   * @param expressionTree
   * @return
   */
  private int countConditions(ExpressionTree expressionTree, List<JavaFileScannerContext.Location> flow) {

    if (expressionTree == null) {
      return 0;
    }

    ExpressionTree expTree = expressionTree;
    if (expressionTree.is(ASSIGNMENT)) {
      expTree = ((AssignmentExpressionTree) expTree).variable();
    }

    if (! isLogicalOp(expTree)) {
      return 0;
    }

    /* top node will be right-most ||
       its right operand will be (in precedence order):
        - the || to the right
        - the && to the right
        - the symbol/expr to be evaluated
     */

    int total = 1;
    BinaryExpressionTree binTree = scootLeft((BinaryExpressionTree) expTree);
    addSecondaryLocation(flow, binTree, 1, 0);

    while (expTree instanceof BinaryExpressionTree){
      binTree = scootLeft((BinaryExpressionTree) expTree);

      ExpressionTree left = binTree.leftOperand();
      ExpressionTree right = binTree.rightOperand();

      if (binTree.kind() != left.kind() && isLogicalOp(left)) {
        total++;
        addSecondaryLocation(flow, binTree, 1, 0);
      }
      if (binTree.kind() != right.kind() && isLogicalOp(right)) {
        total++;
        addSecondaryLocation(flow, binTree, 1, 0);
      }
      if (binTree.parent().is(CONDITIONAL_OR) && right.is(CONDITIONAL_AND)) {
        total++;
        addSecondaryLocation(flow, binTree, 1, 0);
      }
      expTree =  binTree.leftOperand();
    }

    return total;
  }

  private BinaryExpressionTree scootLeft(BinaryExpressionTree binaryExpressionTree) {

    BinaryExpressionTree binTree = binaryExpressionTree;

    if (binTree.is(CONDITIONAL_AND)) {
      while (binTree.leftOperand().is(CONDITIONAL_AND)) {
        binTree = (BinaryExpressionTree) binTree.leftOperand();
      }
    } else if (binTree.is(CONDITIONAL_OR)) {
      ExpressionTree left = binTree.leftOperand();
      while (left.is(CONDITIONAL_OR) && ! isLogicalOp(((BinaryExpressionTree)left).rightOperand())) {
        binTree = (BinaryExpressionTree) binTree.leftOperand();
        left = binTree.leftOperand();
      }
    }
    return binTree;
  }


  private boolean isLogicalOp(ExpressionTree expTree) {
    return expTree.is(CONDITIONAL_AND) || expTree.is(CONDITIONAL_OR);
  }

  private int countTryChains(Tree st, List<JavaFileScannerContext.Location> flow, int nestingLevel) {
    int total = 0;

    if (st.is(Tree.Kind.TRY_STATEMENT)) {
      TryStatementTree tryStatment = (TryStatementTree) st;

      List<CatchTree> catches = tryStatment.catches();
      for (CatchTree catchTree : catches) {
        total++;
        total += nestingLevel;
        addSecondaryLocation(flow, catchTree, total, nestingLevel);
        total += countComplexity(catchTree.block().body(), flow, nestingLevel +1);
      }

      if (tryStatment.finallyBlock() != null && tryStatment.finallyBlock().body() != null) {
        total += countComplexity(tryStatment.finallyBlock().body(), flow, nestingLevel);
      }
    }

    return total;
  }

  private int countBreakAndContinue(Tree st, List<JavaFileScannerContext.Location> flow) {
    if ( (st.is(BREAK_STATEMENT) && ((BreakStatementTree) st).label() != null)
            || (st.is(CONTINUE_STATEMENT) && ((ContinueStatementTree) st).label() != null) ) {
      flow.add(new JavaFileScannerContext.Location("+1", st));
      return 1;

    }

    return 0;
  }

  private Tree getSegmentOfInterest(StatementTree st) {
    switch (st.kind()) {
      case LABELED_STATEMENT:
        return ((LabeledStatementTree)st).statement();
      case VARIABLE:
        VariableTree vt = (VariableTree)st;
        if (vt.initializer() != null) {
          return vt.initializer();
        }
        break;
      case EXPRESSION_STATEMENT:
        if (((ExpressionStatementTree)st).expression() != null && ((ExpressionStatementTree)st).expression().is(Tree.Kind.ASSIGNMENT)) {
          AssignmentExpressionTree aet = (AssignmentExpressionTree)((ExpressionStatementTree) st).expression();
          if (aet.expression() != null && aet.expression().is(Tree.Kind.NEW_CLASS) && ((NewClassTree) aet.expression()).classBody() != null) {
            return ((NewClassTree) aet.expression()).classBody();
          }
        }
        break;
      case RETURN_STATEMENT:
        if (((ReturnStatementTree) st).expression() != null) {
          return ((ReturnStatementTree) st).expression();
        }
        break;
      default:
        break;
    }

    return st;
  }


  private List<StatementTree> getChildren(Tree st) {

    StatementTree statementTree = null;

    switch (st.kind()) {
      case SWITCH_STATEMENT:
        List<StatementTree> children = new ArrayList<>();
        for(CaseGroupTree cgt : ((SwitchStatementTree) st).cases()) {
          children.addAll(cgt.body());
        }
        return children;
      case TRY_STATEMENT:
        return ((TryStatementTree) st).block().body();

      case LAMBDA_EXPRESSION:
        Tree tree = ((LambdaExpressionTree) st).body();
        if (tree.is(BLOCK)) {
          statementTree = (StatementTree) tree;
        }
        break;
      case METHOD:
        statementTree = ((MethodTree) st).block();
        break;

      case IF_STATEMENT:
        statementTree = ((IfStatementTree) st).thenStatement();
        break;

      case FOR_EACH_STATEMENT:
        statementTree = ((ForEachStatement) st).statement();
        break;

      case FOR_STATEMENT:
        statementTree =((ForStatementTree) st).statement();
        break;

      case DO_STATEMENT:
        statementTree = ((DoWhileStatementTree) st).statement();
        break;

      case WHILE_STATEMENT:
        statementTree = ((WhileStatementTree) st).statement();
        break;

      case BLOCK:
        // final `else` clause
        statementTree = (StatementTree) st;
        break;

      default:
        break;
    }


    if (statementTree == null) {
      return new ArrayList<>();
    }

    if (statementTree.is(Tree.Kind.BLOCK)) {
      return ((BlockTree) statementTree).body();
    } else {
      List<StatementTree> list = new ArrayList<>();
      list.add(statementTree);
      return list;
    }

  }

  public void setMax(int max) {
    this.max = max;
  }

}
