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

import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PSet;
import org.sonar.java.se.checks.SyntaxTreeNameFinder;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FlowComputation {

  private final Predicate<Constraint> addToFlow;
  private final Predicate<Constraint> terminateTraversal;
  private final Set<SymbolicValue> symbolicValues;
  private final List<Class<? extends Constraint>> domains;

  private FlowComputation(Set<SymbolicValue> symbolicValues, Predicate<Constraint> addToFlow,
                          Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains) {
    this.addToFlow = addToFlow;
    this.terminateTraversal = terminateTraversal;
    this.symbolicValues = symbolicValues;
    this.domains = domains;
  }

  private static Set<SymbolicValue> computedFrom(@Nullable SymbolicValue symbolicValue) {
    if (symbolicValue == null) {
      return Collections.emptySet();
    }
    Set<SymbolicValue> result = new HashSet<>();
    result.add(symbolicValue);
    symbolicValue.computedFrom().forEach(sv -> result.addAll(computedFrom(sv)));
    return result;
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, Set<SymbolicValue> symbolicValues, Predicate<Constraint> addToFlow,
                                                                Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains) {
    Set<SymbolicValue> allSymbolicValues = symbolicValues.stream()
      .map(FlowComputation::computedFrom)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());

    FlowComputation flowComputation = new FlowComputation(allSymbolicValues, addToFlow, terminateTraversal, domains);
    // FIXME SONARJAVA-2049 getLastEvaluated doesn't work with relational SV
    Symbol trackSymbol = currentNode.programState.getLastEvaluated();
    return flowComputation.run(currentNode, trackSymbol);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal, constraint -> true, domains);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
                                                                List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal, addToFlow, c -> false, domains);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
                                                                Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal == null ? ImmutableSet.of() : ImmutableSet.of(currentVal), addToFlow, terminateTraversal, domains);
  }

  private Set<List<JavaFileScannerContext.Location>> run(final ExplodedGraph.Node node, @Nullable final Symbol trackSymbol) {
    Set<List<JavaFileScannerContext.Location>> flows = new HashSet<>();
    Deque<ExecutionPath> workList = new ArrayDeque<>();

    node.edges().forEach(e -> workList.push(startPath(e, trackSymbol)));
    while (!workList.isEmpty()) {
      ExecutionPath path = workList.pop();
      if (path.finished) {
        flows.add(path.flow);
      } else {
        path.lastEdge.parent.edges().stream()
          .filter(path::notVisited)
          .map(path::addEdge)
          .forEach(workList::push);
      }
    }
    return flows;
  }

  ExecutionPath startPath(ExplodedGraph.Edge edge, @Nullable Symbol trackSymbol) {
    return new ExecutionPath(null, PCollections.emptySet(), trackSymbol, ImmutableList.of(), false).addEdge(edge);
  }

  private class ExecutionPath {
    @Nullable
    final Symbol trackSymbol;
    final ExplodedGraph.Edge lastEdge;
    final PSet<ExplodedGraph.Edge> visited;
    final List<JavaFileScannerContext.Location> flow;
    final boolean finished;

    private ExecutionPath(@Nullable ExplodedGraph.Edge edge, PSet<ExplodedGraph.Edge> visited, @Nullable Symbol trackSymbol, List<JavaFileScannerContext.Location> flow,
                          boolean finished) {
      this.trackSymbol = trackSymbol;
      this.lastEdge = edge;
      this.visited = visited;
      this.flow = flow;
      this.finished = finished;
    }

    ExecutionPath addEdge(ExplodedGraph.Edge edge) {
      ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder = ImmutableList.builder();
      flowBuilder.addAll(flow);
      Symbol newTrackSymbol = addFlowFromLearnedAssociations(edge, flowBuilder);
      Stream<Constraint> constraintStream = addFlowFromLearnedConstraints(edge, flowBuilder);
      return new ExecutionPath(edge, visited.add(edge), newTrackSymbol, flowBuilder.build(), visitedAllParents(edge) || constraintStream.anyMatch(terminateTraversal));
    }

    private boolean visitedAllParents(ExplodedGraph.Edge edge) {
      return edge.parent.edges().stream().allMatch(visited::contains);
    }

    boolean notVisited(ExplodedGraph.Edge e) {
      return !visited.contains(e);
    }

    @Nullable
    private Symbol addFlowFromLearnedAssociations(ExplodedGraph.Edge edge, ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder) {
      if (trackSymbol == null) {
        return null;
      }
      ExplodedGraph.Node parent = edge.parent;
      Optional<LearnedAssociation> learnedAssociation = edge.learnedAssociations().stream()
        .filter(la -> la.symbol().equals(trackSymbol))
        .findFirst();
      if (learnedAssociation.isPresent()) {
        LearnedAssociation la = learnedAssociation.get();
        for (Class<? extends Constraint> domain : domains) {
          Constraint constraint = parent.programState.getConstraint(la.symbolicValue(), domain);
          if (constraint != null) {
            flowBuilder.add(location(parent, String.format("'%s' is assigned %s.", la.symbol().name(), constraint.valueAsString())));
          }
        }
        return parent.programState.getLastEvaluated();
      }
      return trackSymbol;
    }

    private Stream<Constraint> addFlowFromLearnedConstraints(ExplodedGraph.Edge edge, ImmutableList.Builder<JavaFileScannerContext.Location> flow) {
      Set<LearnedConstraint> learnedConstraints = edge.learnedConstraints();
      Stream.Builder<Constraint> builder = Stream.builder();
      for (LearnedConstraint learnedConstraint : learnedConstraints) {
        if (symbolicValues.contains(learnedConstraint.symbolicValue()) && learnedConstraintForDomains(learnedConstraint)) {
          Stream<JavaFileScannerContext.Location> locationStream = learnedConstraintFlow(learnedConstraint, edge);
          locationStream.forEach(flow::add);
          builder.add(learnedConstraint.constraint);
        }
      }
      return builder.build();
    }

    private boolean learnedConstraintForDomains(LearnedConstraint lc) {
      Constraint constraint = lc.constraint;
      return constraint == null || domains.stream().anyMatch(d -> d.isAssignableFrom(constraint.getClass()));
    }

    private Stream<JavaFileScannerContext.Location> learnedConstraintFlow(LearnedConstraint learnedConstraint, ExplodedGraph.Edge edge) {
      Constraint constraint = learnedConstraint.constraint();
      if (constraint == null || !addToFlow.test(constraint)) {
        return Stream.empty();
      }
      ExplodedGraph.Node parent = edge.parent;
      Tree nodeTree = parent.programPoint.syntaxTree();
      if (isMethodInvocationNode(parent)) {
        return methodInvocationFlow(constraint, edge);
      }
      if (nodeTree.is(Tree.Kind.NEW_CLASS)) {
        return Stream.of(location(parent, String.format("Constructor implies '%s'.", constraint.valueAsString())));
      }
      String name = SyntaxTreeNameFinder.getName(nodeTree);
      String message = name == null ? constraint.valueAsString() : String.format("Implies '%s' is %s.", name, constraint.valueAsString());
      return Stream.of(location(parent, message));
    }

    private Stream<JavaFileScannerContext.Location> methodInvocationFlow(Constraint learnedConstraint, ExplodedGraph.Edge edge) {
      ExplodedGraph.Node parent = edge.parent;
      MethodInvocationTree mit = (MethodInvocationTree) parent.programPoint.syntaxTree();
      Stream.Builder<JavaFileScannerContext.Location> flowBuilder = Stream.builder();
      SymbolicValue returnSV = edge.child.programState.peekValue();
      if (symbolicValues.contains(returnSV)) {
        flowBuilder.add(location(parent, String.format("'%s()' returns %s.", mit.symbol().name(), learnedConstraint.valueAsString())));
      }
      SymbolicValue invocationTarget = parent.programState.peekValue(mit.arguments().size());
      if (symbolicValues.contains(invocationTarget)) {
        flowBuilder.add(location(parent, "..."));
      }

      List<Integer> argumentIndices = correspondingArgumentIndices(symbolicValues, parent);
      argumentIndices.stream()
        .map(mit.arguments()::get)
        .map(argTree -> {
          String message = String.format("Implies '%s' is %s.", SyntaxTreeNameFinder.getName(argTree), learnedConstraint.valueAsString());
          return new JavaFileScannerContext.Location(message, argTree);
        })
        .forEach(flowBuilder::add);

      MethodYield selectedMethodYield = edge.child.selectedMethodYield(parent);
      if (selectedMethodYield != null) {
        if (symbolicValues.contains(returnSV)) {
          // to retrieve flow for return value
          argumentIndices.add(-1);
        }
        // FIXME SONARJAVA-2076 consider all flows from yields
        selectedMethodYield.flow(argumentIndices, domains).stream()
          .limit(1)
          .flatMap(List::stream)
          .forEach(flowBuilder::add);
      }
      return flowBuilder.build();
    }

    private boolean isMethodInvocationNode(ExplodedGraph.Node node) {
      // ProgramPoint#syntaxTree will not always return the correct tree, so we need to go to ProgramPoint#block directly
      ProgramPoint pp = node.programPoint;
      if (pp.i < pp.block.elements().size()) {
        Tree tree = pp.block.elements().get(pp.i);
        return tree.is(Tree.Kind.METHOD_INVOCATION);
      }
      return false;
    }

    private List<Integer> correspondingArgumentIndices(Set<SymbolicValue> candidates, ExplodedGraph.Node invocationNode) {
      MethodInvocationTree mit = (MethodInvocationTree) invocationNode.programPoint.syntaxTree();
      List<SymbolicValue> arguments = argumentsUsedForMethodInvocation(invocationNode, mit);
      return IntStream.range(0, arguments.size())
        .filter(i -> candidates.contains(arguments.get(i)))
        .boxed().collect(Collectors.toList());
    }

    private List<SymbolicValue> argumentsUsedForMethodInvocation(ExplodedGraph.Node invocationNode, MethodInvocationTree mit) {
      List<SymbolicValue> values = invocationNode.programState.peekValues(mit.arguments().size());
      return Lists.reverse(values);
    }

    private JavaFileScannerContext.Location location(ExplodedGraph.Node node, String message) {
      return new JavaFileScannerContext.Location(message, node.programPoint.syntaxTree());
    }
  }

  public static Set<List<JavaFileScannerContext.Location>> singleton(String msg, Tree tree) {
    return ImmutableSet.of(ImmutableList.of(new JavaFileScannerContext.Location(msg, tree)));
  }
}
