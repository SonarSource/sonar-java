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
                                                                Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains, @Nullable Symbol trackSymbol) {
    Set<SymbolicValue> allSymbolicValues = symbolicValues.stream()
      .map(FlowComputation::computedFrom)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());

    FlowComputation flowComputation = new FlowComputation(allSymbolicValues, addToFlow, terminateTraversal, domains);
    // FIXME SONARJAVA-2049 getLastEvaluated doesn't work with relational SV
    return flowComputation.run(currentNode, trackSymbol == null ? currentNode.programState.getLastEvaluated() : trackSymbol);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal, constraint -> true, domains);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, List<Class<? extends Constraint>> domains,
                                                                @Nullable Symbol trackSymbol) {
    return flow(currentNode, currentVal == null ? ImmutableSet.of() : ImmutableSet.of(currentVal), c -> true, c -> false, domains, trackSymbol);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
                                                                List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal, addToFlow, c -> false, domains);
  }

  public static Set<List<JavaFileScannerContext.Location>> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
                                                                Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, currentVal == null ? ImmutableSet.of() : ImmutableSet.of(currentVal), addToFlow, terminateTraversal, domains, null);
  }

  private Set<List<JavaFileScannerContext.Location>> run(final ExplodedGraph.Node node, @Nullable final Symbol trackSymbol) {
    Set<List<JavaFileScannerContext.Location>> flows = new HashSet<>();
    Deque<ExecutionPath> workList = new ArrayDeque<>();

    node.edges().stream().flatMap(e -> startPath(e, trackSymbol)).forEach(workList::push);
    while (!workList.isEmpty()) {
      ExecutionPath path = workList.pop();
      if (path.finished) {
        flows.add(path.flow);
      } else {
        path.lastEdge.parent.edges().stream()
          .filter(path::notVisited)
          .flatMap(path::addEdge)
          .forEach(workList::push);
      }
    }
    return flows;
  }

  Stream<ExecutionPath> startPath(ExplodedGraph.Edge edge, @Nullable Symbol trackSymbol) {
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

    Stream<ExecutionPath> addEdge(ExplodedGraph.Edge edge) {
      ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder = ImmutableList.builder();
      flowBuilder.addAll(flow);

      List<JavaFileScannerContext.Location> laFlow = learnedAssociation(edge)
        .map(la -> flowFromLearnedAssociation(la, edge.parent))
        .orElse(ImmutableList.of());
      flowBuilder.addAll(laFlow);

      Symbol newTrackSymbol = newTrackedSymbol(edge);

      Set<LearnedConstraint> learnedConstraints = learnedConstraints(edge);
      List<JavaFileScannerContext.Location> lcFlow = flowFromLearnedConstraints(edge, learnedConstraints);
      flowBuilder.addAll(lcFlow);

      boolean endOfPath = visitedAllParents(edge) || shouldTerminate(learnedConstraints);

      List<JavaFileScannerContext.Location> currentFlow = flowBuilder.build();
      Set<List<JavaFileScannerContext.Location>> yieldsFlows = flowFromYields(edge);
      return yieldsFlows.stream()
        .map(yieldFlow -> ImmutableList.<JavaFileScannerContext.Location>builder().addAll(currentFlow).addAll(yieldFlow).build())
        .map(f -> new ExecutionPath(edge, visited.add(edge), newTrackSymbol, f, endOfPath));
    }

    private List<JavaFileScannerContext.Location> flowFromLearnedConstraints(ExplodedGraph.Edge edge, Set<LearnedConstraint> learnedConstraints) {
      return learnedConstraints.stream()
        .map(lc -> learnedConstraintFlow(lc, edge))
        .flatMap(List::stream)
        .collect(Collectors.toList());
    }

    private boolean shouldTerminate(Set<LearnedConstraint> learnedConstraints) {
      return learnedConstraints.stream().map(LearnedConstraint::constraint).anyMatch(terminateTraversal);
    }

    Optional<LearnedAssociation> learnedAssociation(ExplodedGraph.Edge edge) {
      return edge.learnedAssociations().stream()
        .filter(la -> la.symbol().equals(trackSymbol))
        .findAny();
    }

    List<JavaFileScannerContext.Location> flowFromLearnedAssociation(LearnedAssociation learnedAssociation, ExplodedGraph.Node node) {
      if (trackSymbol == null) {
        return ImmutableList.of();
      }
      ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder = ImmutableList.builder();
      for (Class<? extends Constraint> domain : domains) {
        Constraint constraint = node.programState.getConstraint(learnedAssociation.symbolicValue(), domain);
        if (constraint != null) {
          String message = String.format("'%s' is assigned %s.", learnedAssociation.symbol().name(), constraint.valueAsString());
          flowBuilder.add(new JavaFileScannerContext.Location(message, node.programPoint.syntaxTree()));
        }
      }
      return flowBuilder.build();
    }

    @Nullable
    private Symbol newTrackedSymbol(ExplodedGraph.Edge edge) {
      if (trackSymbol == null) {
        return null;
      }
      if (learnedAssociation(edge).isPresent()) {
        return edge.parent.programState.getLastEvaluated();
      }
      return trackSymbol;
    }

    private boolean visitedAllParents(ExplodedGraph.Edge edge) {
      return edge.parent.edges().stream().allMatch(visited::contains);
    }

    boolean notVisited(ExplodedGraph.Edge e) {
      return !visited.contains(e);
    }

    Set<LearnedConstraint> learnedConstraints(ExplodedGraph.Edge edge) {
      Set<LearnedConstraint> learnedConstraints = edge.learnedConstraints();
      ImmutableSet.Builder<LearnedConstraint> lcByDomainBuilder = ImmutableSet.builder();
      // guarantee that we will keep the same domain order when reporting
      for (Class<? extends Constraint> domain : domains) {
        learnedConstraints.stream()
          .filter(lc -> symbolicValues.contains(lc.symbolicValue()) && hasConstraintForDomain(lc, domain))
          .forEach(lcByDomainBuilder::add);
      }

      return lcByDomainBuilder.build();
    }

    private boolean hasConstraintForDomain(LearnedConstraint lc, Class<? extends Constraint> domain) {
      Constraint constraint = lc.constraint;
      return constraint == null || domain.isAssignableFrom(constraint.getClass());
    }

    private List<JavaFileScannerContext.Location> learnedConstraintFlow(LearnedConstraint learnedConstraint, ExplodedGraph.Edge edge) {
      Constraint constraint = learnedConstraint.constraint();
      if (constraint == null || !addToFlow.test(constraint)) {
        return ImmutableList.of();
      }
      ExplodedGraph.Node parent = edge.parent;
      Tree nodeTree = parent.programPoint.syntaxTree();
      if (isMethodInvocationNode(parent)) {
        return methodInvocationFlow(constraint, edge);
      }
      if (nodeTree.is(Tree.Kind.NEW_CLASS)) {
        return ImmutableList.of(location(parent, String.format("Constructor implies '%s'.", constraint.valueAsString())));
      }
      String name = SyntaxTreeNameFinder.getName(nodeTree);
      String message = name == null ? constraint.valueAsString() : String.format("Implies '%s' is %s.", name, constraint.valueAsString());
      return ImmutableList.of(location(parent, message));
    }

    private List<JavaFileScannerContext.Location> methodInvocationFlow(Constraint learnedConstraint, ExplodedGraph.Edge edge) {
      ExplodedGraph.Node parent = edge.parent;
      MethodInvocationTree mit = (MethodInvocationTree) parent.programPoint.syntaxTree();
      ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder = ImmutableList.builder();
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

      return flowBuilder.build();
    }

    private Set<List<JavaFileScannerContext.Location>> flowFromYields(ExplodedGraph.Edge edge) {
      Set<MethodYield> methodYields = edge.yields();
      if (methodYields.isEmpty()) {
        // return one flow with no elements, nothing will be added to the flow of the current path
        // but this is necessary so path is returned in #addEdge and stays in the worklist in #run
        return ImmutableSet.of(ImmutableList.of());
      }

      List<Integer> argumentIndices = correspondingArgumentIndices(symbolicValues, edge.parent);
      SymbolicValue returnSV = edge.child.programState.peekValue();
      if (symbolicValues.contains(returnSV)) {
        // to retrieve flow for return value
        argumentIndices.add(-1);
      }
      return methodYields.stream()
        .map(y -> y.flow(argumentIndices, domains))
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
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
