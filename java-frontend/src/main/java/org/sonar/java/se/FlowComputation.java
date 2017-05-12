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

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PSet;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.checks.SyntaxTreeNameFinder;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FlowComputation {

  public static final String IMPLIES_MSG = "Implies '%s' is %s.";
  private static final int MAX_FLOW_STEPS = 3_000_000;
  private static final Logger LOG = Loggers.get(ExplodedGraphWalker.class);
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

    PSet<Symbol> trackedSymbols = PCollections.emptySet();
    if (trackSymbol != null) {
      trackedSymbols = trackedSymbols.add(trackSymbol);
    }
    ProgramState currentNodeProgramState = currentNode.programState;
    if (currentNodeProgramState != null) {
      for (SymbolicValue symbolicValue : allSymbolicValues) {
        Symbol symbol = currentNodeProgramState.lastAssociatedSymbols.get(symbolicValue);
        if (symbol != null) {
          trackedSymbols = trackedSymbols.add(symbol);
        }
      }
    }

    FlowComputation flowComputation = new FlowComputation(allSymbolicValues, addToFlow, terminateTraversal, domains);
    return flowComputation.run(currentNode, trackedSymbols);
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

  private Set<List<JavaFileScannerContext.Location>> run(final ExplodedGraph.Node node, PSet<Symbol> trackedSymbols) {
    Set<List<JavaFileScannerContext.Location>> flows = new HashSet<>();
    Deque<ExecutionPath> workList = new ArrayDeque<>();
    node.edges().stream().flatMap(e -> startPath(e, trackedSymbols)).forEach(workList::push);
    int flowSteps = 0;
    Set<ExecutionPath> visited = new HashSet<>(workList);
    while (!workList.isEmpty()) {
      ExecutionPath path = workList.pop();
      if (path.finished) {
        flows.add(path.flow);
      } else {
        path.lastEdge.parent.edges().stream()
          .filter(path::notVisited)
          .flatMap(path::addEdge)
          .forEach(ep -> {
            if(visited.add(ep)) {
              workList.push(ep);
            }
          });
      }
      flowSteps++;
      if(flowSteps == MAX_FLOW_STEPS) {
        LOG.debug("Flow was not able to complete");
        break;
      }
    }
    return flows;
  }

  Stream<ExecutionPath> startPath(ExplodedGraph.Edge edge, PSet<Symbol> trackedSymbols) {
    return new ExecutionPath(null, PCollections.emptySet(), trackedSymbols, ImmutableList.of(), false).addEdge(edge);
  }

  private class ExecutionPath {
    final PSet<Symbol> trackedSymbols;
    final ExplodedGraph.Edge lastEdge;
    final PSet<ExplodedGraph.Edge> visited;
    final List<JavaFileScannerContext.Location> flow;
    final boolean finished;

    private ExecutionPath(@Nullable ExplodedGraph.Edge edge, PSet<ExplodedGraph.Edge> visited, PSet<Symbol> trackedSymbols, List<JavaFileScannerContext.Location> flow,
                          boolean finished) {
      this.trackedSymbols = trackedSymbols;
      this.lastEdge = edge;
      this.visited = visited;
      this.flow = flow;
      this.finished = finished;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ExecutionPath that = (ExecutionPath) o;
      return Objects.equals(trackedSymbols, that.trackedSymbols) &&
        Objects.equals(lastEdge.parent, that.lastEdge.parent) &&
        Objects.equals(flow, that.flow);
    }

    @Override
    public int hashCode() {
      return Objects.hash(trackedSymbols, lastEdge.parent, flow);
    }

    Stream<ExecutionPath> addEdge(ExplodedGraph.Edge edge) {
      ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder = ImmutableList.builder();
      flowBuilder.addAll(flow);

      List<JavaFileScannerContext.Location> laFlow = learnedAssociation(edge)
        .map(la -> flowFromLearnedAssociation(la, edge.parent))
        .orElse(ImmutableList.of());
      flowBuilder.addAll(laFlow);

      PSet<Symbol> newTrackSymbols = newTrackedSymbols(edge);

      Set<LearnedConstraint> learnedConstraints = learnedConstraints(edge);
      List<JavaFileScannerContext.Location> lcFlow = flowFromLearnedConstraints(edge, learnedConstraints);
      flowBuilder.addAll(lcFlow);

      boolean endOfPath = visitedAllParents(edge) || shouldTerminate(learnedConstraints);

      List<JavaFileScannerContext.Location> currentFlow = flowBuilder.build();
      Set<List<JavaFileScannerContext.Location>> yieldsFlows = flowFromYields(edge);
      return yieldsFlows.stream()
        .map(yieldFlow -> ImmutableList.<JavaFileScannerContext.Location>builder().addAll(currentFlow).addAll(yieldFlow).build())
        .map(f -> new ExecutionPath(edge, visited.add(edge), newTrackSymbols, f, endOfPath));
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
        .filter(la -> trackedSymbols.contains(la.symbol))
        .findAny();
    }

    List<JavaFileScannerContext.Location> flowFromLearnedAssociation(LearnedAssociation learnedAssociation, ExplodedGraph.Node node) {
      ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder = ImmutableList.builder();
      for (Class<? extends Constraint> domain : domains) {
        Constraint constraint = node.programState.getConstraint(learnedAssociation.symbolicValue(), domain);
        if (constraint != null) {
          flowBuilder.add(location(node, String.format("'%s' is assigned %s.", learnedAssociation.symbol().name(), constraint.valueAsString())));
        }
      }
      return flowBuilder.build();
    }

    private PSet<Symbol> newTrackedSymbols(ExplodedGraph.Edge edge) {
      Optional<LearnedAssociation> learnedAssociation = learnedAssociation(edge);
      return learnedAssociation.map(la -> {
        PSet<Symbol> newTrackedSymbols = trackedSymbols.remove(la.symbol);
        // if assigning literal, stop tracking symbols, because same SV is reused see SONARJAVA-2164
        if (isLiteralAssignment(edge.parent.programPoint.syntaxTree())) {
          return newTrackedSymbols;
        }
        ProgramState programState = edge.parent.programState;
        Symbol symbol = programState.lastAssociatedSymbols.get(la.symbolicValue());
        if (symbol != null) {
          newTrackedSymbols = newTrackedSymbols.add(symbol);
        } else {
          // take all symbolic values used in this assignment and retrieve corresponding symbols
          for (SymbolicValue sv : computedFrom(la.symbolicValue())) {
            Symbol newSymbol = programState.lastAssociatedSymbols.get(sv);
            if (newSymbol != null) {
              newTrackedSymbols = newTrackedSymbols.add(newSymbol);
            }
          }
        }
        return newTrackedSymbols;
      }).orElse(trackedSymbols);
    }

    private boolean isLiteralAssignment(Tree tree) {
      return tree.is(Tree.Kind.ASSIGNMENT) && ((AssignmentExpressionTree) tree).expression().is(Tree.Kind.NULL_LITERAL, Tree.Kind.BOOLEAN_LITERAL);
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
      if (name == null) {
        return ImmutableList.of();
      }
      return ImmutableList.of(location(parent, String.format(IMPLIES_MSG, name, constraint.valueAsString())));
    }

    private List<JavaFileScannerContext.Location> methodInvocationFlow(Constraint learnedConstraint, ExplodedGraph.Edge edge) {
      ExplodedGraph.Node parent = edge.parent;
      MethodInvocationTree mit = (MethodInvocationTree) parent.programPoint.syntaxTree();
      ImmutableList.Builder<JavaFileScannerContext.Location> flowBuilder = ImmutableList.builder();
      SymbolicValue returnSV = edge.child.programState.peekValue();
      if (symbolicValues.contains(returnSV)) {
        flowBuilder.add(methodInvocationReturnMessage(learnedConstraint, edge, mit.symbol().name()));
      }
      SymbolicValue invocationTarget = parent.programState.peekValue(mit.arguments().size());
      if (symbolicValues.contains(invocationTarget)) {
        String invocationTargetName = SyntaxTreeNameFinder.getName(mit.methodSelect());
        flowBuilder.add(location(parent, String.format(IMPLIES_MSG, invocationTargetName, learnedConstraint.valueAsString())));
      }

      List<Integer> argumentIndices = correspondingArgumentIndices(symbolicValues, parent);
      argumentIndices.stream()
        .map(mit.arguments()::get)
        .map(argTree -> {
          String message = String.format(IMPLIES_MSG, SyntaxTreeNameFinder.getName(argTree), learnedConstraint.valueAsString());
          return new JavaFileScannerContext.Location(message, argTree);
        })
        .forEach(flowBuilder::add);

      return flowBuilder.build();
    }

    private JavaFileScannerContext.Location methodInvocationReturnMessage(Constraint constraint, ExplodedGraph.Edge edge, String methodName) {
      String msg;
      if (isConstraintOnlyPossibleResult(constraint, edge)) {
        msg = String.format("'%s()' returns %s.", methodName, constraint.valueAsString());
      } else {
        msg = String.format("'%s()' can return %s.", methodName, constraint.valueAsString());
      }
      return location(edge.parent, msg);
    }

    private boolean isConstraintOnlyPossibleResult(Constraint constraint, ExplodedGraph.Edge edge) {
      Set<MethodYield> selectedYields = edge.yields();
      if (selectedYields.isEmpty()) {
        // not based on x-procedural analysis, so certainty of constraint is not be guaranteed
        return false;
      }
      MethodBehavior methodBehavior = selectedYields.iterator().next().methodBehavior();
      return methodBehavior.happyPathYields()
        .map(HappyPathYield::resultConstraint)
        .allMatch(resultConstraint -> resultConstraint != null && constraint.equals(resultConstraint.get(constraint.getClass())));
    }

    private Set<List<JavaFileScannerContext.Location>> flowFromYields(ExplodedGraph.Edge edge) {
      Set<MethodYield> methodYields = edge.yields();
      if (methodYields.isEmpty()) {
        // return one flow with no elements, nothing will be added to the flow of the current path
        // but this is necessary so path is returned in #addEdge and stays in the worklist in #run
        return ImmutableSet.of(ImmutableList.of());
      }

      List<Integer> argumentIndices = correspondingArgumentIndices(symbolicValues, edge.parent);

      MethodInvocationTree mit = (MethodInvocationTree) edge.parent.programPoint.syntaxTree();
      // computes flow messages for arguments being passed to the called method
      List<JavaFileScannerContext.Location> passedArgumentsMessages = flowsForPassedArguments(argumentIndices, mit);
      // computes flow messages for arguments changing name within called method
      List<JavaFileScannerContext.Location> changingNameArgumentsMessages = flowsForArgumentsChangingName(argumentIndices, mit);

      SymbolicValue returnSV = edge.child.programState.peekValue();
      if (symbolicValues.contains(returnSV)) {
        // to retrieve flow for return value
        argumentIndices.add(-1);
      }
      if (argumentIndices.isEmpty()) {
        // no need to compute any flow on yields : no arg nor return value are corresponding to tracked SVs
        return ImmutableSet.of(ImmutableList.of());
      }
      return methodYields.stream()
        .map(y -> y.flow(argumentIndices, domains))
        .flatMap(Set::stream)
        .map(flowFromYield -> ImmutableList.<JavaFileScannerContext.Location>builder()
          .addAll(flowFromYield)
          .addAll(changingNameArgumentsMessages)
          .addAll(passedArgumentsMessages)
          .build())
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

  public static List<JavaFileScannerContext.Location> flowsForPassedArguments(List<Integer> argumentIndices, MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    return Lists.reverse(argumentIndices.stream()
      .map(index -> getArgumentIdentifier(mit, index))
      .filter(Objects::nonNull)
      .map(identifierTree -> new JavaFileScannerContext.Location(String.format("'%s' is passed to '%s()'.", identifierTree.name(), methodName), identifierTree))
      .collect(Collectors.toList()));
  }

  public static List<JavaFileScannerContext.Location> flowsForArgumentsChangingName(List<Integer> argumentIndices, MethodInvocationTree mit) {
    List<JavaFileScannerContext.Location> result = new ArrayList<>();

    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) mit.symbol();
    MethodTree declaration = methodSymbol.declaration();
    if (declaration == null) {
      return Collections.emptyList();
    }
    List<VariableTree> methodParameters = declaration.parameters();

    for (Integer argumentIndex : argumentIndices) {
      // do not consider varargs part
      if (methodSymbol.isVarArgs() && argumentIndex >= methodParameters.size() - 1) {
        break;
      }
      IdentifierTree argumentName = getArgumentIdentifier(mit, argumentIndex);
      if (argumentName != null) {
        IdentifierTree parameterIdentifier = methodParameters.get(argumentIndex).simpleName();
        String identifierName = parameterIdentifier.name();
        if (!argumentName.name().equals(identifierName)) {
          result.add(new JavaFileScannerContext.Location(String.format("Implies '%s' has the same value as '%s'.", identifierName, argumentName.name()), parameterIdentifier));
        }
      }
    }
    return Lists.reverse(result);
  }

  @CheckForNull
  public static IdentifierTree getArgumentIdentifier(MethodInvocationTree mit, int index) {
    Arguments arguments = mit.arguments();
    if (index < 0 || index > arguments.size()) {
      return null;
    }
    ExpressionTree expr = ExpressionUtils.skipParentheses(arguments.get(index));
    switch (expr.kind()) {
      case MEMBER_SELECT:
        return ((MemberSelectExpressionTree) expr).identifier();
      case IDENTIFIER:
        return ((IdentifierTree) expr);
      default:
        return null;
    }
  }
}
