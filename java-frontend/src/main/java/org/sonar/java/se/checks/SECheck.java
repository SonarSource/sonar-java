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
package org.sonar.java.se.checks;

import com.google.common.collect.Sets;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class SECheck implements JavaFileScanner {

  private Set<SEIssue> issues = new HashSet<>();
  
  public void init(MethodTree methodTree, CFG cfg){

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

  public void reportIssue(Tree tree, String message, Set<List<JavaFileScannerContext.Location>> secondary) {
    issues.add(issues.stream()
      .filter(seIssue -> seIssue.tree.equals(tree))
      .findFirst()
      .map(seIssue -> {
        seIssue.flows.addAll(secondary);
        return seIssue;
      })
      .orElse(new SEIssue(tree, message, secondary)));
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, SymbolicValue currentVal) {
    List<JavaFileScannerContext.Location> flow = new ArrayList<>();
    if(currentVal instanceof BinarySymbolicValue) {
      Set<JavaFileScannerContext.Location> locations = Sets.newHashSet();
      locations.addAll(flow(currentNode.parent, ((BinarySymbolicValue) currentVal).getLeftOp()));
      locations.addAll(flow(currentNode.parent, ((BinarySymbolicValue) currentVal).getRightOp()));
      flow.addAll(locations);
    }
    ExplodedGraph.Node node = currentNode;
    Symbol lastEvaluated = currentNode.programState.getLastEvaluated();
    while (node != null) {
      ExplodedGraph.Node finalNode = node;
      if(finalNode.programPoint.syntaxTree() != null) {
        node.getLearnedConstraints().stream()
          .map(lc->lc.sv)
          .filter(sv -> sv.equals(currentVal))
          .findFirst()
          .ifPresent(sv -> flow.add(new JavaFileScannerContext.Location("", finalNode.parent.programPoint.syntaxTree())));
        if (lastEvaluated != null) {
          Symbol finalLastEvaluated = lastEvaluated;
          Optional<Symbol> learnedSymbol = node.getLearnedSymbols().stream()
            .map(ls -> ls.symbol)
            .filter(sv -> sv.equals(finalLastEvaluated))
            .findFirst();
          if (learnedSymbol.isPresent()) {
            lastEvaluated = finalNode.parent.programState.getLastEvaluated();
            flow.add(new JavaFileScannerContext.Location("", finalNode.parent.programPoint.syntaxTree()));
          }
        }

      }
      node = node.parent;
    }
    return flow;
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
      this.flows = flows;
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
