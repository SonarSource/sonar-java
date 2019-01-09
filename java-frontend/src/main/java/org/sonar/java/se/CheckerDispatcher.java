/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.se;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

public class CheckerDispatcher implements CheckerContext {
  private final ExplodedGraphWalker explodedGraphWalker;
  private final List<SECheck> checks;
  private int currentCheckerIndex = -1;
  private boolean transition = false;
  private Exception interruptionCause = null;
  Tree syntaxNode;
  // used by walker to store chosen yield when adding a transition from MIT
  @Nullable
  MethodYield methodYield = null;

  public CheckerDispatcher(ExplodedGraphWalker explodedGraphWalker, List<SECheck> checks) {
    this.explodedGraphWalker = explodedGraphWalker;
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
    addTransition(explodedGraphWalker.programState);
  }

  @Override
  public ProgramState getState() {
    return explodedGraphWalker.programState;
  }

  @Override
  public ExplodedGraph.Node getNode() {
    return explodedGraphWalker.node;
  }

  @Override
  public void reportIssue(Tree tree, SECheck check, String message) {
    reportIssue(tree, check, message, Collections.emptySet());
  }

  @Override
  public void reportIssue(Tree tree, SECheck check, String message, Set<Flow> flows) {
    check.reportIssue(tree, message, flows);
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

  private void executePost() {
    this.transition = false;
    if (currentCheckerIndex < checks.size()) {
      explodedGraphWalker.programState = checks.get(currentCheckerIndex).checkPostStatement(this, syntaxNode);
    } else {
      CFG.Block block = (CFG.Block) explodedGraphWalker.programPosition.block;
      if (explodedGraphWalker.programPosition.i< block.elements().size()) {
        explodedGraphWalker.clearStack(block.elements().get(explodedGraphWalker.programPosition.i));
      }
      explodedGraphWalker.enqueue(
        explodedGraphWalker.programPosition.next(),
        explodedGraphWalker.programState, explodedGraphWalker.node.exitPath, methodYield);
      return;
    }
    if (!transition) {
      addTransition(explodedGraphWalker.programState);
    }
  }

  @Override
  public void addExceptionalYield(SymbolicValue target, ProgramState exceptionalState, String exceptionFullyQualifiedName, SECheck check) {
    explodedGraphWalker.addExceptionalYield(target, exceptionalState, exceptionFullyQualifiedName, check);
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

  public void init(MethodTree methodTree, CFG cfg) {
    for (SECheck checker : checks) {
      checker.init(methodTree, cfg);
    }
  }

  @Override
  public ConstraintManager getConstraintManager() {
    return explodedGraphWalker.constraintManager;
  }

  public void interruptedExecution(Exception interruptionCause) {
    this.interruptionCause = interruptionCause;
    checks.forEach(c -> c.interruptedExecution(this));
    this.interruptionCause = null;
  }

  /**
   * Will be not null only when the execution is interrupted, and only during handling of {@link SECheck#interruptedExecution(CheckerContext)}.
   * Rest of the time, returns null
   */
  @CheckForNull
  public Exception interruptionCause() {
    return interruptionCause;
  }

  @Override
  public AlwaysTrueOrFalseExpressionCollector alwaysTrueOrFalseExpressions() {
    return explodedGraphWalker.alwaysTrueOrFalseExpressionCollector();
  }

  @CheckForNull
  public MethodBehavior methodBehavior() {
    return explodedGraphWalker.methodBehavior;
  }

  @CheckForNull
  public MethodBehavior peekMethodBehavior(Symbol.MethodSymbol symbol) {
    return explodedGraphWalker.peekMethodBehavior(symbol);
  }
}
