/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.checks;

import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SECheck implements JavaFileScanner {

  private Set<SEIssue> issues = new HashSet<>();

  public void init(MethodTree methodTree, CFG cfg) {

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

  public void reportIssue(Tree tree, String message, Set<List<JavaFileScannerContext.Location>> flows) {
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

  private static class SEIssue {
    private final Tree tree;
    private final String message;
    private final Set<List<JavaFileScannerContext.Location>> flows;

    public SEIssue(Tree tree, String message, Set<List<JavaFileScannerContext.Location>> flows) {
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
      return flows;
    }
  }
}
