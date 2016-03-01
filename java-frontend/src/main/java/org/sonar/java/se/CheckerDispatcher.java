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
package org.sonar.java.se;

import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.List;

public class CheckerDispatcher implements CheckerContext {
  private final ExplodedGraphWalker explodedGraphWalker;
  private final JavaFileScannerContext context;
  private final List<SECheck> checks;
  private Tree syntaxNode;
  private int currentCheckerIndex = 0;
  private boolean transition = false;

  public CheckerDispatcher(ExplodedGraphWalker explodedGraphWalker, JavaFileScannerContext context, List<SECheck> checks) {
    this.explodedGraphWalker = explodedGraphWalker;
    this.context = context;
    this.checks = checks;
  }

  public boolean executeCheckPreStatement(Tree syntaxNode) {
    this.syntaxNode = syntaxNode;
    ProgramState ps;
    for (SECheck checker : checks) {
      ps = checker.checkPreStatement(this, syntaxNode);
      if (ps == null) {
        return false;
      }
      explodedGraphWalker.programState = ps;
    }
    return true;

  }

  public void executeCheckPostStatement(Tree syntaxNode) {
    this.syntaxNode = syntaxNode;
    ProgramState ps;
    for (SECheck checker : checks) {
      ps = checker.checkPostStatement(this, syntaxNode);
      if (ps == null) {
        throw new IllegalStateException("Post statement processing is not allowed to generate a sink yet!");
      }
      explodedGraphWalker.programState = ps;
    }
  }

  private void executePost() {
    this.transition = false;
    if (currentCheckerIndex < checks.size()) {
      explodedGraphWalker.programState = checks.get(currentCheckerIndex).checkPostStatement(this, syntaxNode);
    } else {
      if (explodedGraphWalker.programPosition.i< explodedGraphWalker.programPosition.block.elements().size()) {
        explodedGraphWalker.clearStack(explodedGraphWalker.programPosition.block.elements().get(explodedGraphWalker.programPosition.i));
      }
      explodedGraphWalker.enqueue(
        new ExplodedGraph.ProgramPoint(explodedGraphWalker.programPosition.block, explodedGraphWalker.programPosition.i + 1),
        explodedGraphWalker.programState, explodedGraphWalker.node.exitPath);
      return;
    }
    if (!transition) {
      addTransition(explodedGraphWalker.programState);
    }
  }

  @Override
  public ProgramState getState() {
    return explodedGraphWalker.programState;
  }

  @Override
  public void reportIssue(Tree tree, SECheck check, String message) {
    reportIssue(tree, check, message, new ArrayList<JavaFileScannerContext.Location>());
  }

  @Override
  public void reportIssue(Tree tree, SECheck check, String message, List<JavaFileScannerContext.Location> locations) {
    ((DefaultJavaFileScannerContext) context).reportSEIssue(check.getClass(), tree, message, locations);
  }

  @Override
  public void addTransition(ProgramState state) {
    ProgramState oldState = explodedGraphWalker.programState;
    explodedGraphWalker.programState = state;
    currentCheckerIndex++;
    executePost();
    currentCheckerIndex--;
    explodedGraphWalker.programState = oldState;
    this.transition = true;
  }

  @Override
  public Object createSink() {
    transition = true;
    return new Object();
  }

  public void executeCheckEndOfExecution() {
    for (SECheck checker : checks) {
      checker.checkEndOfExecution(this);
    }
  }

  public void executeCheckEndOfExecutionPath(ConstraintManager constraintManager) {
    for (SECheck checker : checks) {
      checker.checkEndOfExecutionPath(this, constraintManager);
    }
  }

  public void init() {
    for (SECheck checker : checks) {
      checker.init();
    }
  }

  @Override
  public ConstraintManager getConstraintManager() {
    return explodedGraphWalker.constraintManager;
  }
}
