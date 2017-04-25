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
package org.sonar.java.se;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class AlwaysTrueOrFalseExpressionCollector {

  private Deque<EvaluatedConditions> evaluatedConditions = new LinkedList<>();

  public void init() {
    evaluatedConditions.push(new EvaluatedConditions());
  }

  public void evaluatedToFalse(Tree condition, ExplodedGraph.Node node) {
    evaluatedConditions.peek().evaluatedToFalse(condition, node);
  }

  public void evaluatedToTrue(Tree condition, ExplodedGraph.Node node) {
    evaluatedConditions.peek().evaluatedToTrue(condition, node);
  }

  public void interruptedExecution() {
    evaluatedConditions.pop();
  }

  public CheckerContext.AlwaysTrueOrFalseExpressions alwaysTrueOrFalseExpressions() {
    return evaluatedConditions.peek();
  }

  private static class EvaluatedConditions implements CheckerContext.AlwaysTrueOrFalseExpressions {
    final Multimap<Tree, ExplodedGraph.Node> evaluatedToFalse = HashMultimap.create();
    final Multimap<Tree, ExplodedGraph.Node> evaluatedToTrue = HashMultimap.create();

    void evaluatedToFalse(Tree condition, ExplodedGraph.Node node) {
      evaluatedToFalse.put(condition, node);
    }

    void evaluatedToTrue(Tree condition, ExplodedGraph.Node node) {
      evaluatedToTrue.put(condition, node);
    }

    @Override
    public Set<Tree> alwaysTrue() {
      return Sets.difference(evaluatedToTrue.keySet(), evaluatedToFalse.keySet());
    }

    @Override
    public Set<Tree> alwaysFalse() {
      return Sets.difference(evaluatedToFalse.keySet(), evaluatedToTrue.keySet());
    }

    @Override
    public Set<List<JavaFileScannerContext.Location>> flowForExpression(Tree expression) {
      Collection<ExplodedGraph.Node> nodes = getNodes(expression);
      return collectFlow(nodes);
    }

    private Collection<ExplodedGraph.Node> getNodes(Tree expression) {
      Collection<ExplodedGraph.Node> falseNodes = evaluatedToFalse.get(expression);
      return falseNodes.isEmpty() ? evaluatedToTrue.get(expression) : falseNodes;
    }

    private static Set<List<JavaFileScannerContext.Location>> collectFlow(Collection<ExplodedGraph.Node> nodes) {
      return nodes.stream()
        .map(EvaluatedConditions::flowFromNode)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
    }

    private static Set<List<JavaFileScannerContext.Location>> flowFromNode(ExplodedGraph.Node node) {
      List<Class<? extends Constraint>> domains = Lists.newArrayList(ObjectConstraint.class, BooleanConstraint.class);
      return FlowComputation.flow(node.parent(), node.programState.peekValue(), domains);
    }

  }
}
