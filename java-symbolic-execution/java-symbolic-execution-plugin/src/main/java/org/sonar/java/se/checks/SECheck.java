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
package org.sonar.java.se.checks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class SECheck implements JavaFileScanner {

  protected Set<SEIssue> issues = new HashSet<>();

  public void init(MethodTree methodTree, ControlFlowGraph cfg) {

  }

  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    return context.getState();
  }

  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    return context.getState();
  }

  public void checkEndOfExecution(CheckerContext context) {
    // By default do nothing
  }

  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    // By default do nothing
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    for (SEIssue seIssue : issues) {
      context.reportIssueWithFlow(this, seIssue.getTree(), seIssue.getMessage(), seIssue.getFlows(), null);
    }
    issues.clear();
  }

  public void reportIssue(Tree tree, String message) {
    reportIssue(tree, message, Collections.emptySet());
  }

  public void reportIssue(Tree tree, String message, Set<Flow> flows) {
    issues.add(issues.stream()
      .filter(seIssue -> seIssue.tree.equals(tree))
      .findFirst()
      .map(seIssue -> {
        seIssue.flows.addAll(flows);
        return seIssue;
      })
      .orElse(new SEIssue(tree, message, flows)));
  }

  public void interruptedExecution(CheckerContext context) {
    // By default do nothing
  }

  protected static class SEIssue {
    private final Tree tree;
    private final String message;
    private final Set<Flow> flows;

    public SEIssue(Tree tree, String message, Set<Flow> flows) {
      this.tree = tree;
      this.message = message;
      this.flows = new HashSet<>(flows);
    }

    public Tree getTree() {
      return tree;
    }

    public String getMessage() {
      return message;
    }

    public Set<List<JavaFileScannerContext.Location>> getFlows() {
      Set<List<JavaFileScannerContext.Location>> nonExceptionalFlows = flows.stream().filter(Flow::isNonExceptional).map(Flow::elements).collect(Collectors.toSet());
      if (!nonExceptionalFlows.isEmpty()) {
        // keep only the non-exceptional flows and ignore exceptional ones
        return nonExceptionalFlows;
      }
      return flows.stream().map(Flow::elements).collect(Collectors.toSet());
    }
  }
}
