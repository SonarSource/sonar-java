/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.se.checks;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Note that {@link StreamNotConsumedCheck} is implemented by using constraints set by this check
 */
@Rule(key = "S3959")
public class StreamConsumedCheck extends SECheck {

  public enum StreamPipelineConstraint implements Constraint {
    CONSUMED, NOT_CONSUMED
  }

  private static final Set<String> STREAM_TYPES = ImmutableSet.of("java.util.stream.Stream", "java.util.stream.IntStream", "java.util.stream.LongStream",
    "java.util.stream.DoubleStream");
  private static final MethodMatchers TERMINAL_OPERATIONS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(STREAM_TYPES.toArray(new String[0]))
      .names("forEach", "forEachOrdered", "toArray", "collect", "reduce", "findAny", "findFirst", "count", "min", "max", "anyMatch",
        "allMatch", "noneMatch", "average", "summaryStatistics", "sum")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.util.stream.BaseStream")
      .names("iterator", "spliterator")
      .addWithoutParametersMatcher()
      .build()
  );


  private static final MethodMatchers.NameBuilder JAVA_UTIL_STREAM_BASESTREAM =  MethodMatchers.create()
    .ofSubTypes("java.util.stream.BaseStream");
  private static final MethodMatchers BASE_STREAM_INTERMEDIATE_OPERATIONS = MethodMatchers.or(
    JAVA_UTIL_STREAM_BASESTREAM.names("sequential", "parallel", "unordered").addWithoutParametersMatcher().build(),
    JAVA_UTIL_STREAM_BASESTREAM.names("onClose").withAnyParameters().build());

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.METHOD_REFERENCE)) {
      return handleMethodReference(context, (MethodReferenceTree) syntaxNode);
    }
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      return handleMethodInvocation(context, (MethodInvocationTree) syntaxNode);
    }
    if (syntaxNode.is(Tree.Kind.NEW_CLASS)) {
      return removeConstraintOnArgs(context.getState(), ((NewClassTree) syntaxNode).arguments().size());
    }
    ProgramState state = context.getState();
    if (state.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue) {
      state = removeNotConsumedConstraints(context.getState());
    }
    return state;
  }

  private static ProgramState removeNotConsumedConstraints(ProgramState programState) {
    ProgramState intermediateState = programState;
    for (SymbolicValue notConsumed : intermediateState.getValuesWithConstraints(StreamPipelineConstraint.NOT_CONSUMED)) {
      intermediateState = intermediateState.removeConstraintsOnDomain(notConsumed, StreamPipelineConstraint.class);
    }
    return intermediateState;
  }

  private ProgramState handleMethodInvocation(CheckerContext context, MethodInvocationTree mit) {
    ProgramState programState = context.getState();
    programState = removeConstraintOnArgs(programState, mit.arguments().size());
    SymbolicValue invocationTarget = invocationTarget(programState, mit);
    if ((isIntermediateOperation(mit) || isTerminalOperation(mit))
        && isPipelineConsumed(programState, invocationTarget)) {
      reportIssue(mit, "Refactor this code so that this consumed stream pipeline is not reused.", flow(invocationTarget, context.getNode()));
      return null;
    }
    if (isIntermediateOperation(mit)) {
      // intermediate operations return same stream pipeline, so we reuse SV
      context.getConstraintManager().setValueFactory(() -> invocationTarget);
      return Iterables.getOnlyElement(invocationTarget.setConstraint(programState, StreamPipelineConstraint.NOT_CONSUMED));
    }
    if (isTerminalOperation(mit)) {
      return Iterables.getOnlyElement(invocationTarget.setConstraint(programState, StreamPipelineConstraint.CONSUMED));
    }
    if (mit.symbol().isUnknown()) {
      // lambdas used in pipelines are sometimes not resolved properly, this is to shutdown the noise
      programState = programState.removeConstraintsOnDomain(invocationTarget, StreamPipelineConstraint.class);
    }
    return programState;
  }

  private ProgramState handleMethodReference(CheckerContext context, MethodReferenceTree mrt) {
    ProgramState programState = context.getState();
    if (TERMINAL_OPERATIONS.matches(mrt.method().symbol())) {
      Tree expression = mrt.expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        SymbolicValue ownerSV = programState.getValue(((IdentifierTree) expression).symbol());
        if (ownerSV == null) {
          return programState;
        }
        if (isPipelineConsumed(programState, ownerSV)) {
          reportIssue(mrt, "Refactor this code so that this consumed stream pipeline is not reused.", flow(ownerSV, context.getNode()));
          return null;
        } else {
          return Iterables.getOnlyElement(ownerSV.setConstraint(programState, StreamPipelineConstraint.CONSUMED));
        }
      }
    }
    return programState;
  }

  private static ProgramState removeConstraintOnArgs(ProgramState programState, int argumentCount) {
    ProgramState state = programState;
    for (SymbolicValue arg : programState.peekValues(argumentCount)) {
      state = state.removeConstraintsOnDomain(arg, StreamPipelineConstraint.class);
    }
    return state;
  }

  private static SymbolicValue invocationTarget(ProgramState programState, MethodInvocationTree mit) {
    return programState.peekValue(mit.arguments().size());
  }

  private static boolean isIntermediateOperation(MethodInvocationTree mit) {
    if (BASE_STREAM_INTERMEDIATE_OPERATIONS.matches(mit)) {
      return true;
    }
    Symbol method = mit.symbol();
    return method.isMethodSymbol()
      && !method.isStatic()
      && STREAM_TYPES.contains(method.owner().type().fullyQualifiedName())
      && STREAM_TYPES.contains(((Symbol.MethodSymbol) method).returnType().type().fullyQualifiedName());
  }

  private static boolean isPipelineConsumed(ProgramState programState, SymbolicValue symbolicValue) {
    StreamPipelineConstraint constraint = programState.getConstraint(symbolicValue, StreamPipelineConstraint.class);
    return constraint == StreamPipelineConstraint.CONSUMED;
  }

  private static boolean isTerminalOperation(MethodInvocationTree methodInvocationTree) {
    return TERMINAL_OPERATIONS.matches(methodInvocationTree);
  }

  private static Set<Flow> flow(SymbolicValue invocationTarget, ExplodedGraph.Node node) {
    Set<Flow> flows = FlowComputation.flow(node, Collections.singleton(invocationTarget), StreamPipelineConstraint.CONSUMED::equals, c -> false,
      Collections.singletonList(StreamPipelineConstraint.class), Collections.emptySet());
    // make copy with explicit message
    return flows.stream()
      .map(StreamConsumedCheck::copyFlowWithExplicitMessage)
      .collect(Collectors.toSet());
  }

  private static Flow copyFlowWithExplicitMessage(Flow flow) {
    Flow.Builder flowBuilder = Flow.builder();
    flow.stream().map(l -> new JavaFileScannerContext.Location("Pipeline is consumed here.", flowTree(l.syntaxNode))).forEach(flowBuilder::add);
    return flowBuilder.build();
  }

  private static Tree flowTree(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      ExpressionTree methodSelect = ((MethodInvocationTree) tree).methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) methodSelect).identifier();
      }
    }
    return tree;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState state = context.getState();
    if (isReturningPipeline(syntaxNode) || nonLocalAssignment(syntaxNode)) {
      return state.removeConstraintsOnDomain(state.peekValue(), StreamPipelineConstraint.class);
    }
    return state;
  }

  private static boolean nonLocalAssignment(Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.ASSIGNMENT)) {
      ExpressionTree variable = ((AssignmentExpressionTree) syntaxNode).variable();
      return !variable.is(Tree.Kind.IDENTIFIER) || ((IdentifierTree) variable).symbol().owner().isTypeSymbol();
    }
    return false;
  }

  private static boolean isReturningPipeline(Tree syntaxNode) {
    return syntaxNode.is(Tree.Kind.RETURN_STATEMENT) && ((ReturnStatementTree) syntaxNode).expression() != null;
  }

}
