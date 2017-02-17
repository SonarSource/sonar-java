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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sonar.java.se.checks.SyntaxTreeNameFinder;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FlowComputation {

  private final Predicate<Constraint> addToFlow;
  private final Predicate<Constraint> terminateTraversal;
  private final List<JavaFileScannerContext.Location> flow = new ArrayList<>();
  private final Set<SymbolicValue> symbolicValues;
  private final Set<ExplodedGraph.Node> visited = new HashSet<>();
  private final List<Class<? extends Constraint>> domains;

  private FlowComputation(@Nullable SymbolicValue symbolicValue, Predicate<Constraint> addToFlow,
                          Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains) {
    this.addToFlow = addToFlow;
    this.terminateTraversal = terminateTraversal;
    this.symbolicValues = computedFrom(symbolicValue);
    this.domains = domains;
  }

  private static Set<SymbolicValue> computedFrom(@Nullable SymbolicValue symbolicValue) {
    if (symbolicValue == null) {
      return Collections.emptySet();
    }
    HashSet<SymbolicValue> result = new HashSet<>();
    result.add(symbolicValue);
    symbolicValue.computedFrom().forEach(sv -> result.addAll(computedFrom(sv)));
    return result;
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal, constraint -> true, domains);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal, addToFlow, c -> false, domains);
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
                                                           List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal, addToFlow, c -> false, domains);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
                                                           Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains) {
    FlowComputation flowComputation = new FlowComputation(currentVal, addToFlow, terminateTraversal, domains);
    // FIXME SONARJAVA-2049 getLastEvaluated doesn't work with relational SV
    Symbol trackSymbol = currentNode.programState.getLastEvaluated();
    return flowComputation.run(currentNode, trackSymbol);
  }

  private static class NodeSymbol {
    final ExplodedGraph.Node node;
    final Symbol trackSymbol;

    public NodeSymbol(@Nullable ExplodedGraph.Node node, @Nullable Symbol trackSymbol) {
      this.node = node;
      this.trackSymbol = trackSymbol;
    }
  }

  private Set<List<JavaFileScannerContext.Location>> run(@Nullable final ExplodedGraph.Node node, @Nullable final Symbol trackSymbol) {
    Deque<NodeSymbol> workList = new ArrayDeque<>();
    workList.add(new NodeSymbol(node, trackSymbol));
    while (!workList.isEmpty()) {
      NodeSymbol nodeSymbol = workList.pop();
      ExplodedGraph.Node currentNode = nodeSymbol.node;
      if (currentNode == null || visited.contains(currentNode)) {
        continue;
      }
      visited.add(currentNode);

      Symbol newTrackSymbol = nodeSymbol.trackSymbol;
      if (currentNode.programPoint.syntaxTree() != null) {
        newTrackSymbol = addFlowFromLearnedAssociations(currentNode, newTrackSymbol);
        Stream<Constraint> learnedConstraints = addFlowFromLearnedConstraints(currentNode);
        if (learnedConstraints.anyMatch(terminateTraversal)) {
          continue;
        }
      }
      for (ExplodedGraph.Node parent : currentNode.parents()) {
        workList.push(new NodeSymbol(parent, newTrackSymbol));
      }
    }
    return ImmutableSet.of(flow);
  }

  private Stream<Constraint> addFlowFromLearnedConstraints(ExplodedGraph.Node currentNode) {
    ExplodedGraph.Node parent = currentNode.parent();
    if (parent == null) {
      return Stream.empty();
    }
    return currentNode.learnedConstraints()
      .filter(lc -> symbolicValues.contains(lc.symbolicValue()))
      .filter(this::learnedConstraintForDomains)
      .map(LearnedConstraint::constraint)
      .peek(lc -> learnedConstraintFlow(lc, currentNode, parent).forEach(flow::add));
  }

  private boolean learnedConstraintForDomains(LearnedConstraint lc) {
    Constraint constraint = lc.constraint;
    return constraint == null || domains.stream().anyMatch(d -> d.isAssignableFrom(constraint.getClass()));
  }

  private Stream<JavaFileScannerContext.Location> learnedConstraintFlow(@Nullable Constraint learnedConstraint, ExplodedGraph.Node currentNode, ExplodedGraph.Node parent) {
    if (learnedConstraint == null || !addToFlow.test(learnedConstraint)) {
      return Stream.empty();
    }
    Tree nodeTree = parent.programPoint.syntaxTree();
    if (isMethodInvocationNode(parent)) {
      return methodInvocationFlow(learnedConstraint, currentNode, parent);
    }
    if (nodeTree.is(Tree.Kind.NEW_CLASS)) {
      return Stream.of(location(parent, String.format("Constructor implies '%s'.", learnedConstraint.valueAsString())));
    }
    String name = SyntaxTreeNameFinder.getName(nodeTree);
    String message = name == null ? learnedConstraint.valueAsString() : String.format("Implies '%s' is %s.", name, learnedConstraint.valueAsString());
    return Stream.of(location(parent, message));
  }

  private Stream<JavaFileScannerContext.Location> methodInvocationFlow(Constraint learnedConstraint, ExplodedGraph.Node currentNode, ExplodedGraph.Node parent) {
    MethodInvocationTree mit = (MethodInvocationTree) parent.programPoint.syntaxTree();
    Stream.Builder<JavaFileScannerContext.Location> flowBuilder = Stream.builder();
    SymbolicValue returnSV = currentNode.programState.peekValue();
    if (symbolicValues.contains(returnSV)) {
      flowBuilder.add(location(parent, String.format("'%s()' returns %s.", mit.symbol().name(), learnedConstraint.valueAsString())));
    }
    SymbolicValue invocationTarget = parent.programState.peekValue(mit.arguments().size());
    if (symbolicValues.contains(invocationTarget)) {
      flowBuilder.add(location(parent, "..."));
    }
    int argIdx = correspondingArgumentIndex(symbolicValues, parent);
    if (argIdx != -1) {
      ExpressionTree argTree = mit.arguments().get(argIdx);
      String message = String.format("Implies '%s' is %s.", SyntaxTreeNameFinder.getName(argTree), learnedConstraint.valueAsString());
      flowBuilder.add(new JavaFileScannerContext.Location(message, argTree));
    }
    MethodYield selectedMethodYield = currentNode.selectedMethodYield(parent);
    if (selectedMethodYield != null) {
      Set<List<JavaFileScannerContext.Location>> xProcFlows = selectedMethodYield.flow(argIdx, domains);
      // FIXME SONARJAVA-2076 consider all flows from yields
      xProcFlows.stream().findFirst().ifPresent(f -> f.forEach(flowBuilder::add));
    }
    return flowBuilder.build();
  }

  private static boolean isMethodInvocationNode(ExplodedGraph.Node node) {
    // ProgramPoint#syntaxTree will not always return the correct tree, so we need to go to ProgramPoint#block directly
    ProgramPoint pp = node.programPoint;
    if (pp.i < pp.block.elements().size()) {
      Tree tree = pp.block.elements().get(pp.i);
      return tree.is(Tree.Kind.METHOD_INVOCATION);
    }
    return false;
  }

  private static int correspondingArgumentIndex(Set<SymbolicValue> candidates, ExplodedGraph.Node invocationNode) {
    MethodInvocationTree mit = (MethodInvocationTree) invocationNode.programPoint.syntaxTree();
    List<SymbolicValue> arguments = argumentsUsedForMethodInvocation(invocationNode, mit);
    // FIXME SONARJAVA-2076 whatif multiple candidates match some of the arguments?
    for (SymbolicValue candidate : candidates) {
      int idx = arguments.indexOf(candidate);
      if (idx != -1) {
        return idx;
      }
    }
    return -1;
  }

  private static List<SymbolicValue> argumentsUsedForMethodInvocation(ExplodedGraph.Node invocationNode, MethodInvocationTree mit) {
    List<SymbolicValue> values = invocationNode.programState.peekValues(mit.arguments().size());
    return Lists.reverse(values);
  }
  @Nullable
  private Symbol addFlowFromLearnedAssociations(ExplodedGraph.Node currentNode, @Nullable Symbol trackSymbol) {
    ExplodedGraph.Node parent = currentNode.parent();
    if (trackSymbol == null || parent == null) {
      return null;
    }
    Optional<LearnedAssociation> learnedAssociation = currentNode.learnedAssociations()
      .filter(la -> la.symbol().equals(trackSymbol))
      .findFirst();
    if (learnedAssociation.isPresent()) {
      LearnedAssociation la = learnedAssociation.get();
      for (Class<? extends Constraint> domain : domains) {
        Constraint constraint = parent.programState.getConstraint(la.symbolicValue(), domain);
        if(constraint != null) {
          flow.add(location(parent, String.format("'%s' is assigned %s.", la.symbol().name(), constraint.valueAsString())));
        }
      }
      return parent.programState.getLastEvaluated();
    }
    return trackSymbol;
  }

  private static JavaFileScannerContext.Location location(ExplodedGraph.Node node, String message) {
    return new JavaFileScannerContext.Location(message, node.programPoint.syntaxTree());
  }

  public static Set<List<JavaFileScannerContext.Location>> singleton(String msg, Tree tree) {
    return ImmutableSet.of(ImmutableList.of(new JavaFileScannerContext.Location(msg, tree)));
  }
}
