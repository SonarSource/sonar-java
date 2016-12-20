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
package org.sonar.java.se;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FlowComputation {

  private final Predicate<Constraint> addToFlow;
  private final Predicate<Constraint> terminateTraversal;
  private final List<JavaFileScannerContext.Location> flow = new ArrayList<>();
  private final SymbolicValue symbolicValue;
  private final Set<ExplodedGraph.Node> visited = new HashSet<>();

  private FlowComputation(SymbolicValue symbolicValue, Predicate<Constraint> addToFlow, Predicate<Constraint> terminateTraversal) {
    this.addToFlow = addToFlow;
    this.terminateTraversal = terminateTraversal;
    this.symbolicValue = symbolicValue;
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, SymbolicValue currentVal) {
    return flow(currentNode, currentVal, constraint -> true);
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, SymbolicValue currentVal, Predicate<Constraint> addToFlow) {
    return flow(currentNode, currentVal, addToFlow, c -> false);
  }

  public static List<JavaFileScannerContext.Location> flow(ExplodedGraph.Node currentNode, SymbolicValue currentVal, Predicate<Constraint> addToFlow,
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
    HashSet<JavaFileScannerContext.Location> binSVFlow = new HashSet<>();
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

  private void run(@Nullable final ExplodedGraph.Node currentNode, @Nullable final Symbol trackSymbol) {
    if (currentNode == null || visited.contains(currentNode)) {
      return;
    }
    visited.add(currentNode);

    Symbol newTrackSymbol = trackSymbol;
    if (currentNode.programPoint.syntaxTree() != null) {
      newTrackSymbol = flowFromLearnedSymbols(currentNode, trackSymbol);
      List<Constraint> learnedConstraints = flowFromLearnedConstraints(currentNode);
      if (learnedConstraints.stream().anyMatch(terminateTraversal)) {
        return;
      }
    }
    for (ExplodedGraph.Node parent : currentNode.getParents()) {
      run(parent, newTrackSymbol);
    }
  }

  private List<Constraint> flowFromLearnedConstraints(ExplodedGraph.Node currentNode) {
    List<Constraint> learnedConstraints = currentNode.getLearnedConstraints().stream()
      .filter(lc -> lc.getSv().equals(symbolicValue))
      .map(ExplodedGraph.Node.LearnedConstraint::getConstraint)
      .collect(Collectors.toList());

    if (learnedConstraints.stream().anyMatch(addToFlow)) {
      ExplodedGraph.Node parentNode = currentNode.parent();
      if (parentNode.programPoint.syntaxTree().is(Tree.Kind.METHOD_INVOCATION) && currentNode.programState.selectedYield() != null) {
        flowFromMethodInvocation(currentNode.programState.selectedYield(), parentNode);
      } else {
        flow.add(location(parentNode));
      }
    }
    return learnedConstraints;
  }

  private void flowFromMethodInvocation(MethodYield selectedYield, ExplodedGraph.Node parentNode) {
    List<JavaFileScannerContext.Location> flowFromMethodInvocation;
    String message;
    Symbol argumentSymbol = correspondingArgumentSymbol(symbolicValue, parentNode);
    if (selectedYield.exception) {
      Preconditions.checkNotNull(argumentSymbol, "If an exception occurs, the method does not return correctly and the impacted symbolic value can only be from arguments");
      message = String.format("Exception '%s' thrown from method invocation", selectedYield.exceptionType.name());
      flowFromMethodInvocation = FlowComputation.flow(selectedYield.node, selectedYield.node.programState.getValue(argumentSymbol));
    } else {
      if (argumentSymbol != null) {
        message = "Learns from method call";
        flowFromMethodInvocation = FlowComputation.flow(selectedYield.node, selectedYield.node.programState.getValue(argumentSymbol));
      } else {
        message = "Uses return value";
        flowFromMethodInvocation = FlowComputation.flow(selectedYield.node, selectedYield.node.programState.exitValue());
      }
    }
    flow.add(location(parentNode, String.format("%s [see L#%d].", message, methodDeclarationLine(parentNode))));
    flow.addAll(flowFromMethodInvocation);
  }

  private static int methodDeclarationLine(ExplodedGraph.Node methodInvocationNode) {
    return ((MethodInvocationTree) methodInvocationNode.programPoint.syntaxTree()).symbol().declaration().firstToken().line();
  }

  @CheckForNull
  private static Symbol correspondingArgumentSymbol(SymbolicValue candidate, ExplodedGraph.Node invocationNode) {
    MethodInvocationTree mit = (MethodInvocationTree) invocationNode.programPoint.syntaxTree();
    List<SymbolicValue> arguments = argumentsUsedForMethodInvocation(invocationNode, mit);

    int indexOfCandidate = arguments.indexOf(candidate);
    if (indexOfCandidate >= 0) {
      return ((JavaSymbol.MethodJavaSymbol) mit.symbol()).getParameters().scopeSymbols().get(indexOfCandidate);
    }
    return null;
  }

  private static List<SymbolicValue> argumentsUsedForMethodInvocation(ExplodedGraph.Node invocationNode, MethodInvocationTree mit) {
    List<SymbolicValue> values = new ArrayList<>(invocationNode.programState.peekValues(mit.arguments().size() + 1));

    values.remove(values.size() - 1);
    Collections.reverse(values);
    return values;
  }

  @Nullable
  private Symbol flowFromLearnedSymbols(ExplodedGraph.Node currentNode, @Nullable Symbol trackSymbol) {
    ExplodedGraph.Node parent = currentNode.parent();
    if (trackSymbol == null || parent == null) {
      return null;
    }
    Optional<ExplodedGraph.Node.LearnedValue> learnedValue = currentNode.getLearnedSymbols().stream()
      .filter(lv -> lv.getSymbol().equals(trackSymbol))
      .findFirst();
    if (learnedValue.isPresent()) {
      ExplodedGraph.Node.LearnedValue lv = learnedValue.get();
      Constraint constraint = parent.programState.getConstraint(lv.getSv());
      JavaFileScannerContext.Location location = constraint == null ? location(parent) :
        location(parent, lv.getSymbol().name() + " is assigned " + constraint.valueAsString());
      flow.add(location);
      return parent.programState.getLastEvaluated();
    }
    return trackSymbol;
  }

  private static JavaFileScannerContext.Location location(ExplodedGraph.Node node) {
    return location(node, "...");
  }

  private static JavaFileScannerContext.Location location(ExplodedGraph.Node node, String message) {
    return new JavaFileScannerContext.Location(message, node.programPoint.syntaxTree());
  }

  public static Set<List<JavaFileScannerContext.Location>> singleton(String msg, Tree tree) {
    return ImmutableSet.of(ImmutableList.of(new JavaFileScannerContext.Location(msg, tree)));
  }
}
