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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleLinearRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayDeque;
import java.util.Deque;

@Rule(
  key = "S135",
  name = "Loops should not contain more than a single \"break\" or \"continue\" statement",
  tags = {"brain-overload"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleLinearRemediation(coeff = "20min", effortToFixDescription = "per extra \"break\" or \"continue\" statement")
public class SeveralBreakOrContinuePerLoopCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Deque<Integer> breakAndContinueCounter = new ArrayDeque<Integer>();
  private final Deque<Boolean> currentScopeIsSwitch = new ArrayDeque<Boolean>();
  private int loopCount;

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    loopCount = 0;
    scan(context.getTree());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    enterLoop();
    super.visitForStatement(tree);
    leaveLoop(tree);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    enterLoop();
    super.visitForEachStatement(tree);
    leaveLoop(tree);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    enterLoop();
    super.visitWhileStatement(tree);
    leaveLoop(tree);
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    enterLoop();
    super.visitDoWhileStatement(tree);
    leaveLoop(tree);
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    if (isInLoop() && !isInSwitch()) {
      incrementBreakCounter();
    }
    super.visitBreakStatement(tree);
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    if (isInLoop()) {
      incrementBreakCounter();
    }
    super.visitContinueStatement(tree);
  }

  private boolean isInLoop() {
    return loopCount > 0;
  }

  private boolean isInSwitch() {
    return currentScopeIsSwitch.peek();
  }

  private void incrementBreakCounter() {
    int increment = 1;
    if (!breakAndContinueCounter.isEmpty()) {
      increment += breakAndContinueCounter.pop();
    }
    breakAndContinueCounter.push(increment);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    currentScopeIsSwitch.push(true);
    super.visitSwitchStatement(tree);
    currentScopeIsSwitch.pop();
  }

  private void enterLoop() {
    loopCount++;
    breakAndContinueCounter.push(0);
    currentScopeIsSwitch.push(false);
  }

  private void leaveLoop(Tree tree) {
    int count = 0;
    if (!breakAndContinueCounter.isEmpty()) {
      count = breakAndContinueCounter.pop();
    }
    if (count > 1) {
      double effortToFix = count - 1.0;
      context.addIssue(tree, this, "Reduce the total number of break and continue statements in this loop to use at most one.", effortToFix);
    }
    loopCount--;
    currentScopeIsSwitch.pop();
  }
}
