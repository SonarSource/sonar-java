/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Rule(key = "S135")
public class SeveralBreakOrContinuePerLoopCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Deque<List<Tree>> breakAndContinueCounter = new ArrayDeque<>();
  private final Deque<Boolean> currentScopeIsSwitch = new ArrayDeque<>();
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
    leaveLoop(tree.forKeyword());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    enterLoop();
    super.visitForEachStatement(tree);
    leaveLoop(tree.forKeyword());
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    enterLoop();
    super.visitWhileStatement(tree);
    leaveLoop(tree.whileKeyword());
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    enterLoop();
    super.visitDoWhileStatement(tree);
    leaveLoop(tree.doKeyword());
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    if (isInLoop() && !isInSwitch()) {
      incrementBreakCounter(tree);
    }
    super.visitBreakStatement(tree);
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    if (isInLoop()) {
      incrementBreakCounter(tree);
    }
    super.visitContinueStatement(tree);
  }

  private boolean isInLoop() {
    return loopCount > 0;
  }

  private boolean isInSwitch() {
    return currentScopeIsSwitch.peek();
  }

  private void incrementBreakCounter(Tree tree) {
    if (!breakAndContinueCounter.isEmpty()) {
      breakAndContinueCounter.peek().add(tree);
    }
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    currentScopeIsSwitch.push(true);
    super.visitSwitchStatement(tree);
    currentScopeIsSwitch.pop();
  }

  private void enterLoop() {
    loopCount++;
    breakAndContinueCounter.push(new ArrayList<>());
    currentScopeIsSwitch.push(false);
  }

  private void leaveLoop(Tree primaryLocationTree) {
    List<Tree> breakAndContinues = new ArrayList<>();
    if (!breakAndContinueCounter.isEmpty()) {
      breakAndContinues = breakAndContinueCounter.pop();
    }
    if (breakAndContinues.size() > 1) {
      int effortToFix = breakAndContinues.size() - 1;

      List<JavaFileScannerContext.Location> secondaryLocations = breakAndContinues.stream()
          .map(t -> new JavaFileScannerContext.Location(String.format("\"%s\" statement.", t.is(Tree.Kind.BREAK_STATEMENT) ? "break" : "continue"), t))
        .toList();

      context.reportIssue(this, primaryLocationTree,
        "Reduce the total number of break and continue statements in this loop to use at most one.",
        secondaryLocations,
        effortToFix);
    }
    loopCount--;
    currentScopeIsSwitch.pop();
  }
}
