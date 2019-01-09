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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AlwaysTrueOrFalseExpressionCollector {

  private final Multimap<Tree, ExplodedGraph.Node> falseEvaluations = HashMultimap.create();
  private final Multimap<Tree, ExplodedGraph.Node> trueEvaluations = HashMultimap.create();

  void evaluatedToFalse(Tree condition, ExplodedGraph.Node node) {
    falseEvaluations.put(condition, node);
  }

  void evaluatedToTrue(Tree condition, ExplodedGraph.Node node) {
    trueEvaluations.put(condition, node);
  }

  public Set<Tree> alwaysTrue() {
    return Sets.difference(trueEvaluations.keySet(), falseEvaluations.keySet());
  }

  public Set<Tree> alwaysFalse() {
    return Sets.difference(falseEvaluations.keySet(), trueEvaluations.keySet());
  }

  public Set<Flow> flowForExpression(Tree expression) {
    Collection<ExplodedGraph.Node> nodes = getNodes(expression);
    return collectFlow(nodes);
  }

  private Collection<ExplodedGraph.Node> getNodes(Tree expression) {
    Collection<ExplodedGraph.Node> falseNodes = falseEvaluations.get(expression);
    return falseNodes.isEmpty() ? trueEvaluations.get(expression) : falseNodes;
  }

  private static Set<Flow> collectFlow(Collection<ExplodedGraph.Node> nodes) {
    return nodes.stream()
      .map(AlwaysTrueOrFalseExpressionCollector::flowFromNode)
      .flatMap(Set::stream)
      .filter(f -> !f.isEmpty())
      .collect(Collectors.toSet());
  }

  private static Set<Flow> flowFromNode(ExplodedGraph.Node node) {
    List<Class<? extends Constraint>> domains = Lists.newArrayList(ObjectConstraint.class, BooleanConstraint.class);
    return FlowComputation.flow(node.parent(), node.programState.peekValue(), domains, node.programState.peekValueSymbol().symbol);
  }

  public static Flow addIssueLocation(Flow flow, Tree issueTree, boolean conditionIsAlwaysTrue) {
    return Flow.builder()
      .add(new JavaFileScannerContext.Location("Expression is always " + conditionIsAlwaysTrue + ".", issueTree))
      .addAll(flow)
      .build();
  }

  public static boolean hasUnreachableCode(Tree booleanExpr, boolean isTrue) {
    Tree parent = biggestTreeWithSameEvaluation(booleanExpr, isTrue);
    if (parent.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatementTree = (IfStatementTree) parent;
      return !isTrue || ifStatementTree.elseStatement() != null;
    }
    // Tree.Kind.DO_STATEMENT not considered, because it is always executed at least once
    if (parent.is(Tree.Kind.WHILE_STATEMENT) && !isTrue) {
      return true;
    }
    return parent.is(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  private static Tree biggestTreeWithSameEvaluation(Tree booleanExpr, boolean isTrue) {
    Tree child = booleanExpr;
    Tree parent = booleanExpr.parent();
    while (isBiggerTreeWithSameTruthiness(parent, child, isTrue)) {
      child = parent;
      parent = parent.parent();
    }
    Preconditions.checkState(parent != null, "Error getting parent tree with same evaluation, parent is null");
    return parent;
  }

  private static boolean isBiggerTreeWithSameTruthiness(@Nullable Tree parent, Tree child, boolean isTrue) {
    if (parent == null) {
      return false;
    }
    if (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      return true;
    }
    Tree.Kind operator = isTrue ? Tree.Kind.CONDITIONAL_OR : Tree.Kind.CONDITIONAL_AND;
    return parent.is(operator) && ((BinaryExpressionTree) parent).leftOperand() == child;
  }
}
