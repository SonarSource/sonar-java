/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Iterators;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.symexec.ExecutionState;
import org.sonar.java.symexec.ExpressionEvaluatorVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Rule(
  key = "S2583",
  name = "Conditions should not unconditionally evaluate to \"TRUE\" or to \"FALSE\"",
  tags = {"bug", "cwe", "misra"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("15min")
public class UselessConditionCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    checkIf(Arrays.asList(new ExecutionState()), tree);
  }

  private void checkIf(List<ExecutionState> states, IfStatementTree tree) {
    ExpressionEvaluatorVisitor results = checkCondition(states, tree.condition());
    checkNestedIf(results.trueStates, tree.thenStatement());
    if (tree.elseStatement() != null) {
      checkNestedIf(results.falseStates, tree.elseStatement());
    }
  }

  private void checkNestedIf(List<ExecutionState> states, Tree tree) {
    if (tree.is(Tree.Kind.BLOCK)) {
      Iterator<StatementTree> iterator = (((BlockTree) tree).body()).iterator();
      Tree firstTree = Iterators.getNext(iterator, null);
      if (firstTree != null && firstTree.is(Tree.Kind.IF_STATEMENT)) {
        checkIf(states, (IfStatementTree) firstTree);
      } else {
        scan(firstTree);
      }
      while (iterator.hasNext()) {
        scan(iterator.next());
      }
    } else if (tree.is(Tree.Kind.IF_STATEMENT)) {
      checkIf(states, (IfStatementTree) tree);
    }
  }

  private ExpressionEvaluatorVisitor checkCondition(List<ExecutionState> states, ExpressionTree tree) {
    ExpressionEvaluatorVisitor evaluation = new ExpressionEvaluatorVisitor(states, tree);
    if (evaluation.isAlwaysFalse()) {
      raiseIssue(tree, "false");
    }
    if (evaluation.isAwlaysTrue()) {
      raiseIssue(tree, "true");
    }
    return evaluation;
  }

  private void raiseIssue(ExpressionTree tree, String value) {
    context.addIssue(tree, this, String.format("Change this condition so that it does not always evaluate to \"%s\"", value));
  }

}
