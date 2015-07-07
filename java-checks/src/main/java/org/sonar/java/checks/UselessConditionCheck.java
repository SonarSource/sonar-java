/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

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
    if (!hasSemantic()) {
      return;
    }
    SymbolicEvaluator engine = new SymbolicEvaluator();
    for (Map.Entry<Tree, SymbolicBooleanConstraint> entry : engine.evaluateMethod(context, new ExecutionState(), (MethodTree) tree).entrySet()) {
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

}
