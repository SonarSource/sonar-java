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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.symexec.ExecutionState;
import org.sonar.java.symexec.OverrunException;
import org.sonar.java.symexec.SymbolicEvaluator;
import org.sonar.java.symexec.SymbolicEvaluator.PackedStates;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
  private final Map<Tree, String> issues = new HashMap<>();
  private SymbolicEvaluator engine;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.block() != null) {
      issues.clear();
      try {
        engine = new SymbolicEvaluator() {
          @Override
          public PackedStates evaluateStatement(PackedStates states, StatementTree tree) {
            checkCondition(states, tree);
            return super.evaluateStatement(states, tree);
          }
        };
        engine.evaluateStatement(new PackedStates(Arrays.asList(new ExecutionState())), tree.block());
        for (Map.Entry<Tree, String> issue : issues.entrySet()) {
          context.addIssue(issue.getKey(), this, issue.getValue());
        }
      } catch (OverrunException e) {
      }
    }
  }

  private void checkCondition(PackedStates states, StatementTree tree) {
    if (tree.is(Tree.Kind.IF_STATEMENT)) {
      PackedStates conditionStates = engine.evaluateCondition(states, ((IfStatementTree) tree).condition());
      if (conditionStates.isAlwaysFalse()) {
        raiseIssue(((IfStatementTree) tree).condition(), "false");
      }
      if (conditionStates.isAlwaysTrue()) {
        raiseIssue(((IfStatementTree) tree).condition(), "true");
      }
    }
  }

  private void raiseIssue(ExpressionTree tree, String value) {
    issues.put(tree, String.format("Change this condition so that it does not always evaluate to \"%s\"", value));
  }

}
