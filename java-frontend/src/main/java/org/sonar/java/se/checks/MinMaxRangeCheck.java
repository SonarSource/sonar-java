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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S3065")
public class MinMaxRangeCheck extends SECheck {

  private static final String UPPER = "upper";
  private static final String LOWER = "lower";
  private static final String FLOW_MESSAGE = "Returns the %s bound.";
  private static final String ISSUE_MESSAGE = "Change these chained %s methods invocations, as final results will always be the %s bound.";

  private static final MethodMatchers MIN_MAX_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.Math")
    .names("min", "max")
    .addParametersMatcher(ANY, ANY)
    .build();

  private enum Operation {
    MIN,
    MAX
  }

  private static class NumericalConstraint implements Constraint {
    private final Number value;

    NumericalConstraint(Number value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Number(" + value + ")";
    }
  }

  private static class MinMaxRangeConstraint implements Constraint {
    private final Operation op;
    private final MethodInvocationTree syntaxNode;

    MinMaxRangeConstraint(MethodInvocationTree syntaxNode) {
      this.syntaxNode = syntaxNode;
      this.op = "min".equals(syntaxNode.symbol().name()) ? Operation.MIN : Operation.MAX;
    }

    @Override
    public String toString() {
      return "Range_" + op.name();
    }
  }

  private static class MinMaxValue {
    private final Number value;
    private final Operation op;
    private final MethodInvocationTree syntaxNode;

    MinMaxValue(NumericalConstraint numericalConstraint, MinMaxRangeConstraint minMaxRangeConstraint) {
      value = numericalConstraint.value;
      op = minMaxRangeConstraint.op;
      syntaxNode = minMaxRangeConstraint.syntaxNode;
    }

    @CheckForNull
    static MinMaxValue fromConstraints(@Nullable ConstraintsByDomain constraints) {
      if (constraints == null) {
        return null;
      }
      Constraint minMaxRangeConstraint = constraints.get(MinMaxRangeConstraint.class);
      Constraint numericalConstraint = constraints.get(NumericalConstraint.class);
      if (minMaxRangeConstraint != null && numericalConstraint != null) {
        return new MinMaxValue((NumericalConstraint) numericalConstraint, (MinMaxRangeConstraint) minMaxRangeConstraint);
      }
      return null;
    }
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState programState = context.getState();
    if (!syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      return programState;
    }
    MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
    if (!MIN_MAX_MATCHER.matches(mit)) {
      return programState;
    }
    List<SymbolicValue> args = programState.peekValues(2);
    for (SymbolicValue arg : args) {
      MinMaxRangeConstraint minMaxConstraint = programState.getConstraint(arg, MinMaxRangeConstraint.class);
      NumericalConstraint numericalConstraint = programState.getConstraint(arg, NumericalConstraint.class);
      if (minMaxConstraint == null && numericalConstraint != null) {
        programState = programState.addConstraint(arg, new MinMaxRangeConstraint(mit));
      }
    }
    return programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    // TODO handle float and double
    switch (syntaxNode.kind()) {
      case INT_LITERAL:
        return handleNumericalLiteral(context, LiteralUtils.intLiteralValue((ExpressionTree) syntaxNode));
      case LONG_LITERAL:
        return handleNumericalLiteral(context, LiteralUtils.longLiteralValue((ExpressionTree) syntaxNode));
      case UNARY_MINUS:
      case UNARY_PLUS:
        return handleNumericalLiteral(context, (UnaryExpressionTree) syntaxNode);
      case IDENTIFIER:
        return handleNumericalConstant(context, (IdentifierTree) syntaxNode);
      case METHOD_INVOCATION:
        return handleMinMaxInvocation(context, (MethodInvocationTree) syntaxNode);
      default:
        return context.getState();
    }
  }

  private static ProgramState handleNumericalConstant(CheckerContext context, IdentifierTree syntaxNode) {
    ProgramState programState = context.getState();
    Symbol identifier = syntaxNode.symbol();
    if (!isNumericalConstant(identifier)) {
      return programState;
    }
    SymbolicValue constant = programState.getValue(identifier);
    if (constant == null) {
      return programState;
    }
    NumericalConstraint numericalConstraint = programState.getConstraint(constant, NumericalConstraint.class);
    if (numericalConstraint == null) {
      return JUtils.constantValue(((Symbol.VariableSymbol) identifier))
        .filter(Number.class::isInstance)
        .map(Number.class::cast)
        .map(value -> programState.addConstraint(constant, new NumericalConstraint(value)))
        .orElse(programState);
    }
    return programState;
  }

  private ProgramState handleMinMaxInvocation(CheckerContext context, MethodInvocationTree syntaxNode) {
    if (!MIN_MAX_MATCHER.matches(syntaxNode)) {
      return context.getState();
    }
    ProgramState programState = context.getState();
    ProgramState psBeforeInvocation = context.getNode().programState;

    List<SymbolicValue> args = psBeforeInvocation.peekValues(2);
    List<ConstraintsByDomain> constraintsByArgs = args.stream().map(programState::getConstraints).collect(Collectors.toList());

    checkRangeInconsistencies(context, syntaxNode, constraintsByArgs);

    return context.getState();
  }

  private void checkRangeInconsistencies(CheckerContext context, MethodInvocationTree syntaxNode, List<ConstraintsByDomain> constraintsByArgs) {
    MinMaxValue arg0MinMaxValue = MinMaxValue.fromConstraints(constraintsByArgs.get(0));
    MinMaxValue arg1MinMaxValue = MinMaxValue.fromConstraints(constraintsByArgs.get(1));

    if (arg0MinMaxValue != null && arg1MinMaxValue != null && arg0MinMaxValue.op != arg1MinMaxValue.op) {
      // bounds have been inverted
      Number upperBound = arg0MinMaxValue.op == Operation.MIN ? arg0MinMaxValue.value : arg1MinMaxValue.value;
      Number lowerBound = arg0MinMaxValue.op == Operation.MAX ? arg0MinMaxValue.value : arg1MinMaxValue.value;

      // all the used values are going to be Numbers
      @SuppressWarnings("unchecked")
      int comparedValue = ((Comparable<Number>) lowerBound).compareTo(upperBound);

      if (comparedValue > 0) {
        String issueMessage;
        String secondOpMessage;
        String firstOpMessage;
        if ("min".equals(syntaxNode.symbol().name())) {
          issueMessage = String.format(ISSUE_MESSAGE, "min/max", LOWER);
          firstOpMessage = String.format(FLOW_MESSAGE, UPPER);
          secondOpMessage = String.format(FLOW_MESSAGE, LOWER);
        } else {
          issueMessage = String.format(ISSUE_MESSAGE, "max/min", UPPER);
          firstOpMessage = String.format(FLOW_MESSAGE, LOWER);
          secondOpMessage = String.format(FLOW_MESSAGE, UPPER);
        }
        MethodInvocationTree flowTree = syntaxNode == arg0MinMaxValue.syntaxNode ? arg1MinMaxValue.syntaxNode : arg0MinMaxValue.syntaxNode;
        Set<Flow> flow = Collections.singleton(Flow.builder()
          .add(new JavaFileScannerContext.Location(secondOpMessage, syntaxNode))
          .add(new JavaFileScannerContext.Location(firstOpMessage, flowTree))
          .build());
        context.reportIssue(syntaxNode, this, issueMessage, flow);
      }
    }
  }

  private static ProgramState handleNumericalLiteral(CheckerContext context, @Nullable Number value) {
    ProgramState programState = context.getState();
    if (value == null) {
      return programState;
    }
    return programState.addConstraint(programState.peekValue(), new NumericalConstraint(value));
  }

  private static ProgramState handleNumericalLiteral(CheckerContext context, UnaryExpressionTree syntaxNode) {
    ProgramState previousPS = context.getNode().programState;
    NumericalConstraint knownNumericalConstraint = previousPS.getConstraint(previousPS.peekValue(), NumericalConstraint.class);

    ProgramState programState = context.getState();
    if (knownNumericalConstraint == null) {
      return programState;
    }
    if (syntaxNode.is(Tree.Kind.UNARY_PLUS)) {
      return programState.addConstraint(programState.peekValue(), knownNumericalConstraint);
    }
    Number value = knownNumericalConstraint.value;
    if (value instanceof Integer) {
      value = -1 * value.intValue();
    } else if (value instanceof Long) {
      value = -1L * value.longValue();
    } else if (value instanceof Float) {
      value = -1.0f * value.floatValue();
    } else {
      value = -1.0 * value.doubleValue();
    }
    return programState.addConstraint(programState.peekValue(), new NumericalConstraint(value));
  }

  private static boolean isNumericalConstant(@Nullable Symbol symbol) {
    return symbol != null && isConstant(symbol) && isNumericalPrimitive(symbol);
  }

  private static boolean isNumericalPrimitive(Symbol symbol) {
    Type type = symbol.type();
    return type.isPrimitive() && type.isNumerical();
  }

  private static boolean isConstant(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.isStatic() && symbol.isFinal();
  }

}
