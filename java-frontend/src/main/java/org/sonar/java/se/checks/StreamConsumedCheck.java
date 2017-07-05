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
package org.sonar.java.se.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S3959")
public class StreamConsumedCheck extends SECheck {

  public enum StreamConsumedConstraint implements Constraint {
    CONSUMED;

    @Override
    public String valueAsString() {
      return "consumed";
    }
  }

  private static final Set<String> STREAM_TYPES = ImmutableSet.of("java.util.stream.Stream", "java.util.stream.IntStream", "java.util.stream.LongStream",
    "java.util.stream.DoubleStream");
  private static final MethodMatcherCollection TERMINAL_OPERATIONS = MethodMatcherCollection.create();
  static {
    List<String> terminalMethods = ImmutableList.of("forEach", "forEachOrdered", "toArray", "collect", "reduce", "findAny", "findFirst", "count", "min", "max", "anyMatch",
      "allMatch", "noneMatch", "average", "summaryStatistics");

    STREAM_TYPES.forEach(streamType -> terminalMethods.forEach(method ->
        TERMINAL_OPERATIONS.add(MethodMatcher.create().typeDefinition(streamType).name(method).withAnyParameters())));
  }
  private static final MethodMatcherCollection INTERMEDIATE_OPERATIONS = MethodMatcherCollection.create();
  static {
    List<String> intermediateMethods = ImmutableList.of("filter", "map", "mapToInt", "mapToLong", "mapToDouble", "flatMap", "flatMapToInt", "flatMapToLong", "flatMapToDouble",
      "flatMapToObj", "distinct", "sorted", "peek", "limit", "mapToObj");
    STREAM_TYPES.forEach(streamType -> intermediateMethods.forEach(method ->
      INTERMEDIATE_OPERATIONS.add(MethodMatcher.create().typeDefinition(streamType).name(method).withAnyParameters())));
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (!syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      return context.getState();
    }
    MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
    ProgramState programState = context.getState();
    SymbolicValue invocationTarget = invocationTarget(programState, mit);
    if (isIntermediateOperation(mit)) {
      // intermediate operations return same stream pipeline, so we reuse SV
      context.getConstraintManager().setValueFactory(() -> invocationTarget);
    }
    if ((isIntermediateOperation(mit) || isTerminalOperation(mit)) && isPipelineConsumed(programState, invocationTarget)) {
      reportIssue(syntaxNode, "Refactor this code so that this consumed stream pipeline is not reused.", flow(context, invocationTarget));
    }
    if (isTerminalOperation(mit)) {
      return Iterables.getOnlyElement(invocationTarget.setConstraint(programState, StreamConsumedConstraint.CONSUMED));
    }
    return context.getState();
  }

  private static Set<List<JavaFileScannerContext.Location>> flow(CheckerContext context, SymbolicValue invocationTarget) {
    Set<List<JavaFileScannerContext.Location>> flows = FlowComputation.flow(context.getNode(), invocationTarget, Collections.singletonList(StreamConsumedConstraint.class));
    // make copy with explicit message
    return flows.stream()
      .map(f ->
        f.stream().map(l -> new JavaFileScannerContext.Location("Pipeline is consumed here.", l.syntaxNode)).collect(Collectors.toList()))
      .collect(Collectors.toSet());
  }

  private static SymbolicValue invocationTarget(ProgramState programState, MethodInvocationTree mit) {
    return programState.peekValue(mit.arguments().size());
  }

  private static boolean isIntermediateOperation(MethodInvocationTree mit) {
    return INTERMEDIATE_OPERATIONS.anyMatch(mit);
  }

  private static boolean isPipelineConsumed(ProgramState programState, SymbolicValue symbolicValue) {
    StreamConsumedConstraint constraint = programState.getConstraint(symbolicValue, StreamConsumedConstraint.class);
    return constraint == StreamConsumedConstraint.CONSUMED;
  }

  private static boolean isTerminalOperation(MethodInvocationTree methodInvocationTree) {
    return TERMINAL_OPERATIONS.anyMatch(methodInvocationTree);
  }
}
