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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.cfg.CFG;
import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PSet;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.ExplodedGraph.Node;
import org.sonar.java.se.checks.SyntaxTreeNameFinder;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class FlowComputation {

  private static final String IMPLIES_IS_MSG = "Implies '%s' is %s.";
  private static final String IMPLIES_CAN_BE_MSG = "Implies '%s' can be %s.";
  private static final String IMPLIES_SAME_VALUE = "Implies '%s' has the same value as '%s'.";

  private static final int MAX_FLOW_STEPS = 3_000_000;
  private static final Logger LOG = Loggers.get(ExplodedGraphWalker.class);
  private final Predicate<Constraint> addToFlow;
  private final Predicate<Constraint> terminateTraversal;
  private final Set<SymbolicValue> symbolicValues;
  private final List<Class<? extends Constraint>> domains;
  private final boolean skipExceptionMessages;

  private FlowComputation(Set<SymbolicValue> symbolicValues, Predicate<Constraint> addToFlow,
                          Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains, boolean skipExceptionMessages) {
    this.addToFlow = addToFlow;
    this.terminateTraversal = terminateTraversal;
    this.symbolicValues = symbolicValues;
    this.domains = domains;
    this.skipExceptionMessages = skipExceptionMessages;
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

  // FIXME It is assumed that this will always return set with at least one element, which could be empty, because result is consumed in other places and messages are
  // added to returned flows. This design should be improved.
  public static Set<Flow> flow(ExplodedGraph.Node currentNode, Set<SymbolicValue> symbolicValues, Predicate<Constraint> addToFlow, Predicate<Constraint> terminateTraversal,
    List<Class<? extends Constraint>> domains, Set<Symbol> symbols) {
    return flow(currentNode, symbolicValues, addToFlow, terminateTraversal, domains, symbols, false);
  }

  public static Set<Flow> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, setFromNullable(currentVal), constraint -> true, c -> false, domains, Collections.emptySet(), false);
  }

  public static Set<Flow> flow(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, List<Class<? extends Constraint>> domains, @Nullable Symbol trackSymbol) {
    return flow(currentNode, setFromNullable(currentVal), c -> true, c -> false, domains, setFromNullable(trackSymbol), false);
  }

  public static Set<Flow> flowWithoutExceptions(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
    List<Class<? extends Constraint>> domains) {
    return flow(currentNode, setFromNullable(currentVal), addToFlow, c -> false, domains, Collections.emptySet(), true);
  }

  public static Set<Flow> flowWithoutExceptions(ExplodedGraph.Node currentNode, @Nullable SymbolicValue currentVal, Predicate<Constraint> addToFlow,
    Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains) {
    return flow(currentNode, setFromNullable(currentVal), addToFlow, terminateTraversal, domains, Collections.emptySet(), true);
  }

  private static Set<Flow> flow(ExplodedGraph.Node currentNode, Set<SymbolicValue> symbolicValues, Predicate<Constraint> addToFlow,
    Predicate<Constraint> terminateTraversal, List<Class<? extends Constraint>> domains, Set<Symbol> symbols, boolean skipExceptionMessages) {
    Set<SymbolicValue> allSymbolicValues = symbolicValues.stream()
      .map(FlowComputation::computedFrom)
      .flatMap(Set::stream)
      .collect(Collectors.toSet());

    PSet<Symbol> trackedSymbols = PCollections.emptySet();
    for (Symbol symbol: symbols) {
      trackedSymbols = trackedSymbols.add(symbol);
    }

    if (symbols.isEmpty()) {
      for (SymbolicValue symbolicValue : symbolicValues) {
        for (Symbol symbol : symbolicValue.computedFromSymbols()) {
          trackedSymbols = trackedSymbols.add(symbol);
        }
      }
    }
    FlowComputation flowComputation = new FlowComputation(allSymbolicValues, addToFlow, terminateTraversal, domains, skipExceptionMessages);
    return flowComputation.run(currentNode, trackedSymbols);
  }

  private static <T> Set<T> setFromNullable(@Nullable T val) {
    return val == null ? Collections.emptySet() : Collections.singleton(val);
  }

  private Set<Flow> run(final ExplodedGraph.Node node, PSet<Symbol> trackedSymbols) {
    Set<Flow> flows = new HashSet<>();
    Deque<ExecutionPath> workList = new ArrayDeque<>();
    SameConstraints sameConstraints = new SameConstraints(node, trackedSymbols, domains);
    node.edges().stream().flatMap(e -> startPath(e, trackedSymbols, sameConstraints)).forEach(workList::push);
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

  Stream<ExecutionPath> startPath(ExplodedGraph.Edge edge, PSet<Symbol> trackedSymbols, SameConstraints sameConstraints) {
    return new ExecutionPath(null, PCollections.emptySet(), trackedSymbols, sameConstraints, Flow.empty(), false).addEdge(edge);
  }

  private static class SameConstraints {
    private final List<Class<? extends Constraint>> domains;
    private final Node node;
    private PSet<Symbol> symbolsHavingAlwaysSameConstraints;

    SameConstraints(ExplodedGraph.Node startNode, PSet<Symbol> trackedSymbols, List<Class<? extends Constraint>> domains) {
      this.domains = domains;
      this.node = startNode;
      this.symbolsHavingAlwaysSameConstraints = PCollections.emptySet();

      findSymbolsHavingAlwaysSameConstraints(trackedSymbols);
    }

    SameConstraints(SameConstraints knownSameConstraints, PSet<Symbol> newTrackedSymbols) {
      this.domains = knownSameConstraints.domains;
      this.node = knownSameConstraints.node;
      this.symbolsHavingAlwaysSameConstraints = knownSameConstraints.symbolsHavingAlwaysSameConstraints;

      findSymbolsHavingAlwaysSameConstraints(newTrackedSymbols);
    }

    private void findSymbolsHavingAlwaysSameConstraints(PSet<Symbol> trackedSymbols) {
      trackedSymbols.forEach(symbol -> {
        if (!symbolsHavingAlwaysSameConstraints.contains(symbol) && hasAlwaysSameConstraints(symbol)) {
          symbolsHavingAlwaysSameConstraints = symbolsHavingAlwaysSameConstraints.add(symbol);
        }
      });
    }

    private boolean hasAlwaysSameConstraints(Symbol symbol) {
      return domains.stream().allMatch(domain -> sameConstraintWhenSameProgramPoint(node, symbol, domain));
    }

    private static boolean sameConstraintWhenSameProgramPoint(ExplodedGraph.Node currentNode, Symbol symbol, Class<? extends Constraint> domain) {
      ProgramState programState = currentNode.programState;
      SymbolicValue sv = programState.getValue(symbol);
      if (sv == null) {
        return false;
      }
      Constraint constraint = programState.getConstraint(sv, domain);
      if (constraint == null) {
        return false;
      }
      Collection<Node> siblingNodes = currentNode.siblings();
      return siblingNodes.stream()
        .map(node -> node.programState)
        .allMatch(ps -> {
          SymbolicValue siblingSV = ps.getValue(symbol);
          if (siblingSV == null) {
            return false;
          }
          Constraint siblingConstraint = ps.getConstraint(siblingSV, domain);
          return constraint.equals(siblingConstraint);
        });
    }

    public boolean hasAlwaysSameConstraint(@Nullable Symbol symbol) {
      return symbol != null && symbolsHavingAlwaysSameConstraints.contains(symbol);
    }
  }

  private class ExecutionPath {
    final PSet<Symbol> trackedSymbols;
    final SameConstraints sameConstraints;
    final ExplodedGraph.Edge lastEdge;
    final PSet<ExplodedGraph.Edge> visited;
    final Flow flow;
    final boolean finished;

    private ExecutionPath(@Nullable ExplodedGraph.Edge edge, PSet<ExplodedGraph.Edge> visited, PSet<Symbol> trackedSymbols, SameConstraints sameConstraints,
                          Flow flow, boolean finished) {
      this.trackedSymbols = trackedSymbols;
      this.sameConstraints = sameConstraints;
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
      Flow.Builder flowBuilder = Flow.builder();
      flowBuilder.addAll(flow);

      Flow laFlow = learnedAssociation(edge)
        .map(la -> flowFromLearnedAssociation(la, edge.parent))
        .orElse(Flow.empty());
      flowBuilder.addAll(laFlow);

      PSet<Symbol> newTrackSymbols = newTrackedSymbols(edge);
      SameConstraints newSameConstraints = newTrackSymbols == trackedSymbols ? sameConstraints : new SameConstraints(sameConstraints, newTrackSymbols);

      if (!skipExceptionMessages) {
        flowFromThrownException(edge).ifPresent(loc -> {
          flowBuilder.setAsExceptional();
          flowBuilder.add(loc);
        });
        flowFromCaughtException(edge).ifPresent(loc -> {
          flowBuilder.setAsExceptional();
          flowBuilder.add(loc);
        });
      }

      Set<LearnedConstraint> learnedConstraints = learnedConstraints(edge);
      Flow lcFlow = flowFromLearnedConstraints(edge, filterRedundantObjectDomain(learnedConstraints));
      flowBuilder.addAll(lcFlow);

      boolean endOfPath = visitedAllParents(edge) || shouldTerminate(learnedConstraints);

      if (endOfPath) {
        flowBuilder.addAll(flowForNullableMethodParameters(edge.parent));
      }

      Flow currentFlow = flowBuilder.build();
      Set<Flow> yieldsFlows = flowFromYields(edge);
      if (yieldsFlows.isEmpty()) {
        return Stream.of(new ExecutionPath(edge, visited.add(edge), newTrackSymbols, newSameConstraints, Flow.of(currentFlow), endOfPath));
      }
      return yieldsFlows.stream()
        .map(yieldFlow -> Flow.builder().addAll(currentFlow).addAll(yieldFlow).build())
        .map(f -> new ExecutionPath(edge, visited.add(edge), newTrackSymbols, newSameConstraints, f, endOfPath));
    }

    private Optional<JavaFileScannerContext.Location> flowFromThrownException(ExplodedGraph.Edge edge) {
      SymbolicValue peekValue = edge.child.programState.peekValue();
      if (peekValue instanceof SymbolicValue.ExceptionalSymbolicValue
        && (isMethodInvocationNode(edge.parent) || isDivByZeroExceptionalYield(edge))) {
        Type type = ((SymbolicValue.ExceptionalSymbolicValue) peekValue).exceptionType();
        String msg = String.format("%s is thrown.", exceptionName(type));
        return Optional.of(location(edge.parent, msg));
      }
      return Optional.empty();
    }

    private boolean isDivByZeroExceptionalYield(ExplodedGraph.Edge edge) {
      Tree tree = edge.parent.programPoint.syntaxTree();
      return tree != null && tree.is(Tree.Kind.DIVIDE, Tree.Kind.DIVIDE_ASSIGNMENT);
    }

    private Optional<JavaFileScannerContext.Location> flowFromCaughtException(ExplodedGraph.Edge edge) {
      ProgramPoint programPoint = edge.parent.programPoint;
      if (((CFG.Block) programPoint.block).isCatchBlock() && programPoint.i == 0) {
        VariableTree catchVariable = ((VariableTree) programPoint.syntaxTree());
        SymbolicValue.CaughtExceptionSymbolicValue caughtSv = ((SymbolicValue.CaughtExceptionSymbolicValue) edge.child.programState.getValue(catchVariable.symbol()));
        Preconditions.checkNotNull(caughtSv, "Caught exception not found in program state");
        Type exceptionType = caughtSv.exception().exceptionType();
        return Optional.of(location(edge.parent, String.format("%s is caught.", exceptionName(exceptionType))));
      }
      return Optional.empty();
    }

    private String exceptionName(@Nullable Type type) {
      if (type == null || type.isUnknown()) {
        return "Exception";
      }
      return "'" + type.name() + "'";
    }

    private Set<LearnedConstraint> filterRedundantObjectDomain(Set<LearnedConstraint> learnedConstraints) {
      Map<SymbolicValue, Long> constraintsBySV = learnedConstraints.stream()
        .collect(Collectors.groupingBy(LearnedConstraint::symbolicValue, Collectors.counting()));
      return learnedConstraints.stream()
        .flatMap(lc -> isConstraintFromObjectDomain(lc.constraint()) && constraintsBySV.get(lc.symbolicValue()) > 1 ? Stream.empty() : Stream.of(lc))
        .collect(Collectors.toSet());
    }

    private boolean isConstraintFromObjectDomain(@Nullable Constraint constraint) {
      return constraint instanceof ObjectConstraint;
    }

    private Flow flowForNullableMethodParameters(ExplodedGraph.Node node) {
      if (!node.edges().isEmpty() || !domains.contains(ObjectConstraint.class)) {
        return Flow.empty();
      }
      Flow.Builder flowBuilder = Flow.builder();
      trackedSymbols.forEach(symbol -> {
        SymbolicValue sv = node.programState.getValue(symbol);
        if (sv == null) {
          return;
        }
        ObjectConstraint startConstraint = node.programState.getConstraint(sv, ObjectConstraint.class);
        if (startConstraint != null && isMethodParameter(symbol)) {
          String msg = IMPLIES_CAN_BE_MSG;
          if (ObjectConstraint.NOT_NULL == startConstraint) {
            msg = "Implies '%s' can not be %s.";
          }
          flowBuilder.add(new JavaFileScannerContext.Location(String.format(msg, symbol.name(), "null"), ((VariableTree) symbol.declaration()).simpleName()));
        }
      });
      return flowBuilder.build();
    }

    private boolean isMethodParameter(Symbol symbol) {
      Symbol owner = symbol.owner();
      return owner.isMethodSymbol() && ((Symbol.MethodSymbol) owner).declaration().parameters().contains(symbol.declaration());
    }

    private Flow flowFromLearnedConstraints(ExplodedGraph.Edge edge, Set<LearnedConstraint> learnedConstraints) {
      // FIXME SONARJAVA-2303 calling distinct is temporary workaround to avoid duplicated messages when constraint is reported on relational SV and also on its operand
      Flow.Builder flowBuilder = Flow.builder();
      learnedConstraints.stream()
        .map(lc -> learnedConstraintFlow(lc, edge))
        .flatMap(Flow::stream)
        .distinct()
        .forEach(flowBuilder::add);
      return flowBuilder.build();
    }

    private boolean shouldTerminate(Set<LearnedConstraint> learnedConstraints) {
      return learnedConstraints.stream().map(LearnedConstraint::constraint).anyMatch(terminateTraversal);
    }

    private Optional<LearnedAssociation> learnedAssociation(ExplodedGraph.Edge edge) {
      return edge.learnedAssociations().stream()
        .filter(la -> trackedSymbols.contains(la.symbol))
        .findAny();
    }

    private Flow flowFromLearnedAssociation(LearnedAssociation learnedAssociation, ExplodedGraph.Node node) {
      ProgramState programState = node.programState;
      Preconditions.checkState(programState != null, "Learned association with null state in parent node of the edge.");
      Symbol rhsSymbol = symbolFromStack(learnedAssociation.symbolicValue(), programState);
      if (rhsSymbol != null) {
        return Flow.of(location(node, String.format(IMPLIES_SAME_VALUE, learnedAssociation.symbol().name(), rhsSymbol.name())));
      }
      Flow.Builder flowBuilder = Flow.builder();
      ConstraintsByDomain allConstraints = programState.getConstraints(learnedAssociation.symbolicValue());
      Collection<Constraint> constraints = filterByDomains(allConstraints);
      boolean isPrimitive = learnedAssociation.symbol.type().isPrimitive();
      for (Constraint constraint : constraints) {
        if (isPrimitive && constraint == ObjectConstraint.NOT_NULL) {
          // don't add message about booleans being NOT_NULL, it's obvious
          continue;
        }
        String symbolName = learnedAssociation.symbol().name();
        String msg;
        if (assigningNullFromTernary(node) || (assigningFromMethodInvocation(node) && assignedFromYieldWithUncertainResult(constraint, node))) {
          msg = IMPLIES_CAN_BE_MSG;
        } else {
          msg = IMPLIES_IS_MSG;
        }
        flowBuilder.add(location(node, String.format(msg, symbolName, constraint.valueAsString())));
      }
      return flowBuilder.build();
    }

    private Collection<Constraint> filterByDomains(@Nullable ConstraintsByDomain allConstraints) {
      if (allConstraints == null) {
        return Collections.emptySet();
      }
      Map<Class<? extends  Constraint>, Constraint> constraints = new LinkedHashMap<>();
      for (Class<? extends Constraint> domain : domains) {
        Constraint constraint = allConstraints.get(domain);
        if (constraint != null) {
          constraints.put(domain, constraint);
        }
      }
      if (constraints.size() > 1) {
        constraints.remove(ObjectConstraint.class);
      }
      return constraints.values();
    }

    private boolean assigningNullFromTernary(Node node) {
      ExpressionTree expr = getInitializer(node);
      return isTernaryWithNullBranch(expr);
    }

    @CheckForNull
    private ExpressionTree getInitializer(Node node) {
      Tree tree = node.programPoint.syntaxTree();
      ExpressionTree expr;
      switch (tree.kind()) {
        case VARIABLE:
          expr = ((VariableTree) tree).initializer();
          break;
        case ASSIGNMENT:
          expr = ((AssignmentExpressionTree) tree).expression();
          break;
        default:
          expr = null;
      }
      return expr;
    }

    private boolean isTernaryWithNullBranch(@Nullable ExpressionTree expressionTree) {
      if (expressionTree == null) {
        return false;
      }
      ExpressionTree expr = ExpressionUtils.skipParentheses(expressionTree);
      if (expr.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
        ConditionalExpressionTree cet = (ConditionalExpressionTree) expr;
        return ExpressionUtils.isNullLiteral(cet.trueExpression()) ^ ExpressionUtils.isNullLiteral(cet.falseExpression());
      }
      return false;
    }

    private boolean assigningFromMethodInvocation(Node node) {
      ExpressionTree expr = getInitializer(node);
      return isMethodInvocation(expr);
    }

    private boolean isMethodInvocation(@Nullable ExpressionTree expressionTree) {
      return expressionTree != null && ExpressionUtils.skipParentheses(expressionTree).is(Tree.Kind.METHOD_INVOCATION);
    }

    private boolean assignedFromYieldWithUncertainResult(Constraint constraint, Node node) {
      return node.edges().stream().noneMatch(edge -> isConstraintOnlyPossibleResult(constraint, edge));
    }

    private PSet<Symbol> newTrackedSymbols(ExplodedGraph.Edge edge) {
      Optional<LearnedAssociation> learnedAssociation = learnedAssociation(edge);
      return learnedAssociation.map(la -> {
        PSet<Symbol> newTrackedSymbols = trackedSymbols.remove(la.symbol);
        ProgramState programState = edge.parent.programState;
        Symbol symbol = symbolFromStack(la.symbolicValue(), programState);
        if (symbol != null) {
          newTrackedSymbols = newTrackedSymbols.add(symbol);
        } else {
          for (Symbol s : la.symbolicValue().computedFromSymbols()) {
            newTrackedSymbols = newTrackedSymbols.add(s);
          }
        }
        return newTrackedSymbols;
      }).orElse(trackedSymbols);
    }

    @CheckForNull
    private Symbol symbolFromStack(SymbolicValue symbolicValue, @Nullable ProgramState programState) {
      if (programState != null && programState.peekValue() == symbolicValue) {
        return programState.peekValueSymbol().symbol;
      }
      return null;
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
      return domain.isAssignableFrom(lc.constraint.getClass());
    }

    private Flow learnedConstraintFlow(LearnedConstraint learnedConstraint, ExplodedGraph.Edge edge) {
      Constraint constraint = learnedConstraint.constraint();
      if (!addToFlow.test(constraint)) {
        return Flow.empty();
      }
      if (constraint == ObjectConstraint.NOT_NULL && learnedConstraint.sv instanceof BinarySymbolicValue) {
        // don't report on binary expression to be not null, it's obvious
        return Flow.empty();
      }
      ExplodedGraph.Node parent = edge.parent;
      Tree nodeTree = parent.programPoint.syntaxTree();
      if (isMethodInvocationNode(parent)) {
        return methodInvocationFlow(constraint, edge);
      }
      if (nodeTree.is(Tree.Kind.NEW_CLASS)) {
        return Flow.of(location(parent, String.format("Constructor implies '%s'.", constraint.valueAsString())));
      }
      Symbol finalField = learnedConstraintOnInitializedFinalField(nodeTree);
      if (finalField != null) {
        // constraints on final fields are set on the fly by the EGW when encountering them for the first time
        String msg = String.format(IMPLIES_IS_MSG, finalField.name(), constraint.valueAsString());
        return Flow.of(new JavaFileScannerContext.Location(msg, ((VariableTree) finalField.declaration()).initializer()));
      }

      Symbol trackedSymbol = getSymbol(parent.programState, learnedConstraint.sv);
      String name = trackedSymbol != null ? trackedSymbol.name() : SyntaxTreeNameFinder.getName(nodeTree);
      if (name == null) {
        // unable to deduce name of element on which we learn a constraint. Nothing is reported
        return Flow.empty();
      }
      String msg;
      if (ObjectConstraint.NULL == constraint && !sameConstraints.hasAlwaysSameConstraint(trackedSymbol)) {
        msg = IMPLIES_CAN_BE_MSG;
      } else {
        msg = IMPLIES_IS_MSG;
      }
      return Flow.of(location(parent, String.format(msg, name, constraint.valueAsString())));
    }

    @CheckForNull
    private Symbol getSymbol(ProgramState programState, SymbolicValue sv) {
      SymbolicValue peekValue = programState.peekValue();
      if (sv.equals(peekValue)) {
        return programState.peekValueSymbol().symbol;
      }
      if (peekValue instanceof BinarySymbolicValue) {
        BinarySymbolicValue bsv = (BinarySymbolicValue) peekValue;
        if (sv.equals(bsv.getRightOp())) {
          return bsv.rightSymbol();
        }
        return bsv.leftSymbol();
      }
      return null;
    }

    @CheckForNull
    private Symbol learnedConstraintOnInitializedFinalField(Tree syntaxTree) {
      Symbol result = null;
      if (syntaxTree.is(Tree.Kind.IDENTIFIER)) {
        result = ((IdentifierTree) syntaxTree).symbol();
      } else if (syntaxTree.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) syntaxTree;
        if (ExpressionUtils.isSelectOnThisOrSuper(mset)) {
          result = mset.identifier().symbol();
        }
      }
      if (isFinalFieldWithInitializer(result)) {
        return result;
      }
      return null;
    }

    private boolean isFinalFieldWithInitializer(@Nullable Symbol symbol) {
      if (symbol != null && symbol.isVariableSymbol() && symbol.owner().isTypeSymbol() && symbol.isFinal()) {
        VariableTree declaration = ((Symbol.VariableSymbol) symbol).declaration();
        return declaration != null && declaration.initializer() != null;
      }
      return false;
    }

    private Flow methodInvocationFlow(Constraint learnedConstraint, ExplodedGraph.Edge edge) {
      ExplodedGraph.Node parent = edge.parent;
      MethodInvocationTree mit = (MethodInvocationTree) parent.programPoint.syntaxTree();
      Flow.Builder flowBuilder = Flow.builder();
      SymbolicValue returnSV = edge.child.programState.peekValue();
      if (symbolicValues.contains(returnSV)) {
        flowBuilder.add(methodInvocationReturnMessage(learnedConstraint, edge, mit.symbol().name()));
      }
      SymbolicValue invocationTarget = parent.programState.peekValue(mit.arguments().size());
      if (symbolicValues.contains(invocationTarget)) {
        String invocationTargetName = SyntaxTreeNameFinder.getName(mit.methodSelect());
        flowBuilder.add(location(parent, String.format(IMPLIES_IS_MSG, invocationTargetName, learnedConstraint.valueAsString())));
      }

      List<Integer> argumentIndices = correspondingArgumentIndices(symbolicValues, parent);
      argumentIndices.stream()
        .map(mit.arguments()::get)
        .map(argTree -> {
          String message = String.format(IMPLIES_IS_MSG, SyntaxTreeNameFinder.getName(argTree), learnedConstraint.valueAsString());
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

    private Set<Flow> flowFromYields(ExplodedGraph.Edge edge) {
      Set<MethodYield> methodYields = edge.yields();
      if (methodYields.isEmpty()) {
        return Collections.emptySet();
      }

      List<Integer> argumentIndices = correspondingArgumentIndices(symbolicValues, edge.parent);

      MethodInvocationTree mit = (MethodInvocationTree) edge.parent.programPoint.syntaxTree();
      // computes flow messages for arguments being passed to the called method
      Flow passedArgumentsMessages = flowsForPassedArguments(argumentIndices, mit);
      // computes flow messages for arguments changing name within called method
      Flow changingNameArgumentsMessages = flowsForArgumentsChangingName(argumentIndices, mit);

      SymbolicValue returnSV = edge.child.programState.peekValue();
      if (symbolicValues.contains(returnSV)) {
        // to retrieve flow for return value
        argumentIndices.add(-1);
      }
      if (argumentIndices.isEmpty()) {
        // no need to compute any flow on yields : no arg nor return value are corresponding to tracked SVs
        return Collections.emptySet();
      }
      return methodYields.stream()
        .map(y -> y.flow(argumentIndices, domains))
        .flatMap(Set::stream)
        .filter(f -> !f.isEmpty())
        .map(flowFromYield -> Flow.builder()
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
        Tree tree = ((CFG.Block) pp.block).elements().get(pp.i);
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

  public static Flow flowsForPassedArguments(List<Integer> argumentIndices, MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    Flow.Builder flowBuilder = Flow.builder();
    argumentIndices.stream()
      .map(index -> getArgumentIdentifier(mit, index))
      .filter(Objects::nonNull)
      .map(identifierTree -> new JavaFileScannerContext.Location(String.format("'%s' is passed to '%s()'.", identifierTree.name(), methodName), identifierTree))
      .forEach(flowBuilder::add);
    return flowBuilder.build().reverse();
  }

  public static Flow flowsForArgumentsChangingName(List<Integer> argumentIndices, MethodInvocationTree mit) {

    JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) mit.symbol();
    MethodTree declaration = methodSymbol.declaration();
    if (declaration == null) {
      return Flow.empty();
    }
    Flow.Builder flowBuilder = Flow.builder();
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
          flowBuilder.add(new JavaFileScannerContext.Location(String.format(IMPLIES_SAME_VALUE, identifierName, argumentName.name()), parameterIdentifier));
        }
      }
    }
    return flowBuilder.build().reverse();
  }

  @CheckForNull
  public static IdentifierTree getArgumentIdentifier(MethodInvocationTree mit, int index) {
    Arguments arguments = mit.arguments();
    if (index < 0 || index > arguments.size()) {
      throw new IllegalArgumentException("index must be within arguments range.");
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
