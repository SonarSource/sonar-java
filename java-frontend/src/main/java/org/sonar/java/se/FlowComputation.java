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
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FlowComputation {

  private final Predicate<Constraint> addToFlow;
  private final Predicate<Constraint> terminateTraversal;
  private final List<JavaFileScannerContext.Location> flow = new ArrayList<>();
  private final SymbolicValue symbolicValue;
  private final Set<ExplodedGraph.Node> visited = new HashSet<>();

  private FlowComputation(@Nullable SymbolicValue symbolicValue, Predicate<Constraint> addToFlow, Predicate<Constraint> terminateTraversal) {
    this.addToFlow = addToFlow;
    this.terminateTraversal = terminateTraversal;
    this.symbolicValue = symbolicValue;
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal) {
    return flow(currentNode, currentVal, constraint -> true);
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow) {
    return flow(currentNode, currentVal, addToFlow, c -> false);
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
    Predicate<Constraint> terminateTraversal) {
    FlowComputation flowComputation = new FlowComputation(currentVal, addToFlow, terminateTraversal);

    Symbol trackSymbol = currentNode.programState.getLastEvaluated();
    if (currentVal instanceof BinarySymbolicValue) {
      Set<JavaFileScannerContext.Location> binSVFlow = flowComputation.flowFromBinarySV(currentNode, (BinarySymbolicValue) currentVal, trackSymbol);
      flowComputation.flow.addAll(binSVFlow);
    }
    flowComputation.run(currentNode, trackSymbol);
    return flowComputation.flow;
  }

  private Set<JavaFileScannerContext.Location> flowFromBinarySV(ExplodedGraph.Node currentNode, BinarySymbolicValue binarySV, Symbol trackSymbol) {
    Set<JavaFileScannerContext.Location> binSVFlow = new LinkedHashSet<>();
    FlowComputation left = fork(binarySV.getLeftOp());
    left.run(currentNode.parent(), trackSymbol);
    binSVFlow.addAll(left.flow);
    FlowComputation right = fork(binarySV.getRightOp());
    right.run(currentNode.parent(), trackSymbol);
    binSVFlow.addAll(right.flow);
    return binSVFlow;
  }

  private FlowComputation fork(SymbolicValue symbolicValue) {
    return new FlowComputation(symbolicValue, addToFlow, terminateTraversal);
  }

  private static class NodeSymbol {
    final ExplodedGraph.Node node;
    final Symbol trackSymbol;

    public NodeSymbol(@Nullable ExplodedGraph.Node node, @Nullable Symbol trackSymbol) {
      this.node = node;
      this.trackSymbol = trackSymbol;
    }
  }

  private void run(@Nullable final ExplodedGraph.Node node, @Nullable final Symbol trackSymbol) {
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
        newTrackSymbol = addFlowFromLearnedSymbols(currentNode, newTrackSymbol);
        Stream<Constraint> learnedConstraints = addFlowFromLearnedConstraints(currentNode);
        if (learnedConstraints.anyMatch(terminateTraversal)) {
          continue;
        }
      }
      for (ExplodedGraph.Node parent : currentNode.getParents()) {
        workList.push(new NodeSymbol(parent, newTrackSymbol));
      }
    }
  }

  private Stream<Constraint> addFlowFromLearnedConstraints(ExplodedGraph.Node currentNode) {
    ExplodedGraph.Node parent = currentNode.parent();
    if (parent == null) {
      return Stream.empty();
    }
    return currentNode.getLearnedConstraints().stream()
      .filter(lc -> lc.symbolicValue().equals(symbolicValue))
      .map(LearnedConstraint::constraint)
      .peek(lc -> learnedConstraintFlow(lc, currentNode, parent).forEach(flow::add));
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
    if (currentNode.programState.peekValue() == symbolicValue) {
      flowBuilder.add(location(parent, String.format("'%s()' returns %s.", mit.symbol().name(), learnedConstraint.valueAsString())));
    }
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      SymbolicValue methodIdentifier = parent.programState.peekValues(mit.arguments().size()).get(mit.arguments().size());
      if (methodIdentifier == symbolicValue) {
        flowBuilder.add(location(parent, "..."));
      }
    }
    int argIdx = correspondingArgumentIndex(symbolicValue, parent);
    if (argIdx != -1) {
      ExpressionTree argTree = mit.arguments().get(argIdx);
      String message = String.format("Implies '%s' is %s.", SyntaxTreeNameFinder.getName(argTree), learnedConstraint.valueAsString());
      flowBuilder.add(new JavaFileScannerContext.Location(message, argTree));
    }
    MethodYield selectedMethodYield = currentNode.selectedMethodYield(parent);
    if (selectedMethodYield != null) {
      selectedMethodYield.flow(argIdx).forEach(flowBuilder::add);
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

  private static int correspondingArgumentIndex(SymbolicValue candidate, ExplodedGraph.Node invocationNode) {
    MethodInvocationTree mit = (MethodInvocationTree) invocationNode.programPoint.syntaxTree();
    List<SymbolicValue> arguments = argumentsUsedForMethodInvocation(invocationNode, mit);
    return arguments.indexOf(candidate);
  }

  private static List<SymbolicValue> argumentsUsedForMethodInvocation(ExplodedGraph.Node invocationNode, MethodInvocationTree mit) {
    List<SymbolicValue> values = invocationNode.programState.peekValues(mit.arguments().size());
    return Lists.reverse(values);
  }
  @Nullable
  private Symbol addFlowFromLearnedSymbols(ExplodedGraph.Node currentNode, @Nullable Symbol trackSymbol) {
    ExplodedGraph.Node parent = currentNode.parent();
    if (trackSymbol == null || parent == null) {
      return null;
    }
    Optional<LearnedAssociation> learnedAssociation = currentNode.getLearnedSymbols().stream()
      .filter(lv -> lv.symbol().equals(trackSymbol))
      .findFirst();
    if (learnedAssociation.isPresent()) {
      LearnedAssociation la = learnedAssociation.get();
      Constraint constraint = parent.programState.getConstraint(la.symbolicValue());
      String message = constraint == null ? "..." : String.format("'%s' is assigned %s.", la.symbol().name(), constraint.valueAsString());
      flow.add(location(parent, message));
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
