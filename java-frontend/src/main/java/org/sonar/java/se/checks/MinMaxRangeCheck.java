/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3065")
public class MinMaxRangeCheck extends SECheck {

  private static final MethodMatcher MIN_MAX_MATCHER = MethodMatcher.create()
    .typeDefinition("java.lang.Math")
    .name(name -> "min".equals(name) || "max".equals(name))
    .addParameter(TypeCriteria.anyType()).addParameter(TypeCriteria.anyType());

  private enum Operation {
    MIN, MAX;
  }

  private static class NumberConstraint implements Constraint {
    private final Number value;

    NumberConstraint(Number value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Number_" + value;
    }
  }

  private static class MinMaxRangeConstraint implements Constraint {
    private final Operation op;

    MinMaxRangeConstraint(MethodInvocationTree mit) {
      this.op = "min".equals(mit.symbol().name()) ? Operation.MIN : Operation.MAX;
    }

    @Override
    public String toString() {
      return "Range_" + op.name();
    }
  }

  private static class MinMaxValue {
    private final Number value;
    private final Operation op;

    MinMaxValue(NumberConstraint numberConstraint, MinMaxRangeConstraint minMaxRangeConstraint) {
      this.value = numberConstraint.value;
      this.op = minMaxRangeConstraint.op;
    }

    @CheckForNull
    static MinMaxValue fromConstraints(@Nullable ConstraintsByDomain constraints) {
      if (constraints == null) {
        return null;
      }
      Constraint minMaxRangeConstraint = constraints.get(MinMaxRangeConstraint.class);
      Constraint numberConstraint = constraints.get(NumberConstraint.class);
      if (minMaxRangeConstraint != null && numberConstraint != null) {
        return new MinMaxValue((NumberConstraint) numberConstraint, (MinMaxRangeConstraint) minMaxRangeConstraint);
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
      NumberConstraint numberConstraint = programState.getConstraint(arg, NumberConstraint.class);
      if (minMaxConstraint == null && numberConstraint != null) {
        programState = programState.addConstraint(arg, new MinMaxRangeConstraint(mit));
      }
    }
    return programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.IDENTIFIER)) {
      return handleNumberConstraint(context, (IdentifierTree) syntaxNode);
    }
    if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      return handleMinMaxInvocation(context, (MethodInvocationTree) syntaxNode);
    }
    return context.getState();
  }

  private static ProgramState handleNumberConstraint(CheckerContext context, IdentifierTree syntaxNode) {
    ProgramState programState = context.getState();
    Symbol identifier = syntaxNode.symbol();
    if (isNumericalConstant(identifier)) {
      SymbolicValue constant = programState.getValue(identifier);
      if (constant != null) {
        NumberConstraint numberConstraint = programState.getConstraint(constant, NumberConstraint.class);
        Optional<Object> constantValue = ((JavaSymbol.VariableJavaSymbol) identifier).constantValue();
        if (numberConstraint == null && constantValue.isPresent()) {
          programState = programState.addConstraint(constant, new NumberConstraint((Number) constantValue.get()));
        }
      }
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

    // min()/max() methods can produce either a or b
    SymbolicValue minMaxResult = programState.peekValue();
    List<ProgramState> nexts = constraintsByArgs.stream()
      .filter(Objects::nonNull)
      .map(argConstraints -> programState.addConstraints(minMaxResult, argConstraints))
      .collect(Collectors.toList());

    if (nexts.isEmpty()) {
      return programState;
    }
    if (nexts.size() == 1) {
      // one result necessarily have previous known constraint
      context.addTransition(nexts.get(0));
      // other is unknown
      return programState;
    }
    // both constraints are propagated
    context.addTransition(nexts.get(1));
    return nexts.get(0);
  }

  private void checkRangeInconsistencies(CheckerContext context, MethodInvocationTree syntaxNode, List<ConstraintsByDomain> constraintsByArgs) {
    MinMaxValue arg0MinMaxValue = MinMaxValue.fromConstraints(constraintsByArgs.get(0));
    MinMaxValue arg1MinMaxValue = MinMaxValue.fromConstraints(constraintsByArgs.get(1));

    if (arg0MinMaxValue != null && arg1MinMaxValue != null && arg0MinMaxValue.op != arg1MinMaxValue.op) {
      // bounds have been inverted
      Number upperBound = arg0MinMaxValue.op == Operation.MIN ? arg0MinMaxValue.value : arg1MinMaxValue.value;
      Number lowerBound = arg0MinMaxValue.op == Operation.MAX ? arg0MinMaxValue.value : arg1MinMaxValue.value;
      int comparedValue = ((Comparable<Number>) lowerBound).compareTo(upperBound);
      if (comparedValue == 0) {
        context.reportIssue(syntaxNode, this, "using always same value");
      }
      if (comparedValue > 0) {
        context.reportIssue(syntaxNode, this, "returning always upper or lower bound");
      }
    }
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
