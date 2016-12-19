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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S2583")
public class ConditionAlwaysTrueOrFalseCheck extends SECheck {

  private Deque<EvaluatedConditions> evaluatedConditions = new LinkedList<>();

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    evaluatedConditions.push(new EvaluatedConditions());
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    EvaluatedConditions ec = evaluatedConditions.pop();
    for (Tree condition : Sets.difference(ec.evaluatedToFalse.keySet(), ec.evaluatedToTrue.keySet())) {
      context.reportIssue(condition, this, "Change this condition so that it does not always evaluate to \"false\"",
        collectFlow(condition, ec.evaluatedToFalse, false));
    }
    for (Tree condition : Sets.difference(ec.evaluatedToTrue.keySet(), ec.evaluatedToFalse.keySet())) {
      context.reportIssue(condition, this, "Change this condition so that it does not always evaluate to \"true\"",
        collectFlow(condition, ec.evaluatedToTrue, true));
    }
  }

  private static Set<List<JavaFileScannerContext.Location>> collectFlow(Tree condition, Multimap<Tree, ExplodedGraph.Node> nodes, boolean conditionIsAlwaysTrue) {
    Set<List<JavaFileScannerContext.Location>> flows = nodes.get(condition).stream().map(node -> FlowComputation.flow(node.parent(), node.programState.peekValue()))
      .collect(Collectors.toSet());
    flows.forEach(f -> f.add(new JavaFileScannerContext.Location("Condition is always " + conditionIsAlwaysTrue, condition)));
    return flows;
  }

  public void evaluatedToFalse(Tree condition, ExplodedGraph.Node node) {
    evaluatedConditions.peek().evaluatedToFalse(condition, node);
  }

  public void evaluatedToTrue(Tree condition, ExplodedGraph.Node node) {
    evaluatedConditions.peek().evaluatedToTrue(condition, node);
  }

  @Override
  public void interruptedExecution(CheckerContext context) {
    evaluatedConditions.pop();
  }

  private static class EvaluatedConditions {
    private final Multimap<Tree, ExplodedGraph.Node> evaluatedToFalse = HashMultimap.create();
    private final Multimap<Tree, ExplodedGraph.Node> evaluatedToTrue = HashMultimap.create();

    void evaluatedToFalse(Tree condition, ExplodedGraph.Node node) {
      evaluatedToFalse.put(condition, node);
    }

    void evaluatedToTrue(Tree condition, ExplodedGraph.Node node) {
      evaluatedToTrue.put(condition, node);
    }

  }
}
