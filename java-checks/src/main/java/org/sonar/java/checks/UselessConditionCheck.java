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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.symexec.ExecutionState;
import org.sonar.java.symexec.SymbolicBooleanConstraint;
import org.sonar.java.symexec.SymbolicEvaluator;
import org.sonar.java.symexec.SymbolicExecutionCheck;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Rule(
  key = "S2583",
  name = "Conditions should not unconditionally evaluate to \"TRUE\" or to \"FALSE\"",
  tags = {"bug", "cwe", "misra"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("15min")
public class UselessConditionCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    Check check = new Check();
    SymbolicEvaluator.evaluateMethod(new ExecutionState(), (MethodTree) tree, check);
    for (Map.Entry<Tree, SymbolicBooleanConstraint> entry : check.result.entrySet()) {
      switch (entry.getValue()) {
        case FALSE:
          raiseIssue(entry.getKey(), "false");
          break;
        case TRUE:
          raiseIssue(entry.getKey(), "true");
          break;
        default:
          break;
      }
    }
  }

  private void raiseIssue(Tree tree, String value) {
    addIssue(tree, String.format("Change this condition so that it does not always evaluate to \"%s\"", value));
  }

  private class Check extends SymbolicExecutionCheck {
    private Map<Tree, SymbolicBooleanConstraint> result = new HashMap<>();

    @Override
    protected void onCondition(ExecutionState executionState, Tree tree, SymbolicBooleanConstraint constraint) {
      result.put(tree, constraint.union(result.get(tree)));
    }
  }

}
