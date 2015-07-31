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

import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayDeque;
import java.util.Deque;

@Rule(
  key = "S128",
  name = "Switch cases should end with an unconditional \"break\" statement",
  tags = {"cert", "cwe", "misra", "pitfall"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("10min")
public class SwitchCaseWithoutBreakCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  private final Deque<CaseGroupTree> invalidCaseGroups = new ArrayDeque<>();
  private CaseGroupTree currentTree = null;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    scan(tree.expression());
    if (!tree.cases().isEmpty()){
      scan(tree.cases().subList(0, tree.cases().size() - 1));
    }
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    currentTree = tree;
    invalidCaseGroups.push(tree);

    super.visitCaseGroup(tree);

    if (invalidCaseGroups.remove(tree)) {
      context.addIssue(Iterables.getLast(tree.labels()), this, "End this switch case with an unconditional break, return or throw statement.");
    }
    currentTree = invalidCaseGroups.peek();
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    super.visitBreakStatement(tree);
    markSwitchCasesAsCompliant();
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    super.visitReturnStatement(tree);
    markSwitchCasesAsCompliant();
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    super.visitThrowStatement(tree);
    markSwitchCasesAsCompliant();
  }

  private void markSwitchCasesAsCompliant() {
    invalidCaseGroups.remove(currentTree);
  }

}
