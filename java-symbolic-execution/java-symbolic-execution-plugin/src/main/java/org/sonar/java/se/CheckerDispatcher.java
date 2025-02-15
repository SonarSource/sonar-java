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
package org.sonar.java.se;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph.Block;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.performance.measure.PerformanceMeasure;

public class CheckerDispatcher implements CheckerContext {
  private final ExplodedGraphWalker explodedGraphWalker;
  private final List<SECheck> checks;
  private int currentCheckerIndex = -1;
  private boolean transition = false;
  private Exception interruptionCause = null;
  private final JavaFileScannerContext scannerContext;
  Tree syntaxNode;
  // used by walker to store chosen yield when adding a transition from MIT
  @Nullable
  MethodYield methodYield = null;

  public CheckerDispatcher(ExplodedGraphWalker explodedGraphWalker, List<SECheck> checks, JavaFileScannerContext scannerContext) {
    this.explodedGraphWalker = explodedGraphWalker;
    this.checks = checks;
    this.scannerContext = scannerContext;
  }

  public boolean executeCheckPreStatement(Tree syntaxNode) {
    this.syntaxNode = syntaxNode;
    ProgramState ps;
    for (SECheck checker : checks) {
      PerformanceMeasure.Duration checkerDuration = PerformanceMeasure.start(checker);
      ps = checker.checkPreStatement(this, syntaxNode);
      checkerDuration.stop();
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
      Block block = explodedGraphWalker.programPosition.block;
      if (explodedGraphWalker.programPosition.i < block.elements().size()) {
        explodedGraphWalker.cleanupStack(block.elements().get(explodedGraphWalker.programPosition.i));
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
      PerformanceMeasure.Duration checkerDuration = PerformanceMeasure.start(checker);
      checker.checkEndOfExecution(this);
      checkerDuration.stop();
    }
  }

  public void executeCheckEndOfExecutionPath(ConstraintManager constraintManager) {
    for (SECheck checker : checks) {
      PerformanceMeasure.Duration checkerDuration = PerformanceMeasure.start(checker);
      checker.checkEndOfExecutionPath(this, constraintManager);
      checkerDuration.stop();
    }
  }

  public void init(MethodTree methodTree, ControlFlowGraph cfg) {
    for (SECheck checker : checks) {
      PerformanceMeasure.Duration checkerDuration = PerformanceMeasure.start(checker);
      checker.init(methodTree, cfg);
      checkerDuration.stop();
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

  @Override
  public JavaFileScannerContext getScannerContext() {
    return scannerContext;
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
