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
package org.sonar.java.se.xproc;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.java.collections.PMap;
import org.sonar.java.se.ExplodedGraph.Node;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExceptionalCheckBasedYield extends ExceptionalYield {

  private final Class<? extends SECheck> check;
  private final SymbolicValue svCausingException;
  private final boolean isMethodVarargs;

  public ExceptionalCheckBasedYield(SymbolicValue svCausingException, Type exceptionType, Class<? extends SECheck> check, Node node, MethodBehavior behavior) {
    super(node, behavior);
    this.check = check;
    this.svCausingException = svCausingException;
    this.isMethodVarargs = behavior.isMethodVarArgs();
    Preconditions.checkArgument(exceptionType != null, "Exception type is required");
    super.setExceptionType(exceptionType);
  }

  @Override
  public Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
    Supplier<SymbolicValue> svSupplier) {

    if (parameterConstraintsMatchExactly(invocationArguments, invocationTypes, programState)) {
      // Only uses these yields when parameter constraints are matching exactly.
      // These yields are triggering issues in rules, so we don't want to use them to make the engine learn wrong constraints.
      // For instance: if a parameter 'a' is required to be NULL to trigger an exception and raise an issue, we can't use the yield
      // from a program state where there is no constraint on the SV provided as parameter.
      return super.statesAfterInvocation(invocationArguments, invocationTypes, programState, svSupplier);
    }
    return Stream.empty();
  }

  private boolean parameterConstraintsMatchExactly(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState) {
    if (!applicableOnVarArgs(invocationTypes)) {
      // VarArgs method invoked with the last parameter being not an array, but an item which will be wrapped in the array
      return false;
    }

    for (int index = 0; index < parametersConstraints.size(); index++) {
      PMap<Class<? extends Constraint>, Constraint> yieldConstraint = parametersConstraints.get(index);
      PMap<Class<? extends Constraint>, Constraint> stateConstraint = argumentConstraint(invocationArguments, programState, index);
      if (!yieldConstraint.isEmpty() && !yieldConstraint.equals(stateConstraint)) {
        // If there is a constraint on a parameter, we need to have the same constraint in the current program state,
        // in order to avoid wrongly learning from this yield and thus raising FPs.
        return false;
      }
    }

    return true;
  }

  private boolean applicableOnVarArgs(List<Type> invocationTypes) {
    int numberParametersYield = parametersConstraints.size();
    int numberArgumentsInCall = invocationTypes.size();

    if (numberParametersYield > numberArgumentsInCall) {
      // VarArgs method called without variadic parameter
      return true;
    }
    if (parametersConstraints.get(numberParametersYield - 1) == null) {
      // no constraint on the last parameter on yield side
      return true;
    }
    if (numberParametersYield != numberArgumentsInCall) {
      // there is a constraint on last parameter, but varArgs method called with multiple arguments in variadic part
      return false;
    }
    // compatible number of parameters, type must be compatible on arguments side
    Type lastArgumentType = invocationTypes.get(numberArgumentsInCall - 1);
    return !isMethodVarargs || (lastArgumentType.isArray() || lastArgumentType.is("<nulltype>"));
  }

  @CheckForNull
  private static PMap<Class<? extends Constraint>, Constraint> argumentConstraint(List<SymbolicValue> invocationArguments, ProgramState programState, int index) {
    if (index < invocationArguments.size()) {
      return programState.getConstraints(invocationArguments.get(index));
    }
    return null;
  }

  @Override
  public void setExceptionType(Type exceptionType) {
    throw new UnsupportedOperationException("Exception type can not be changed");
  }

  @Nonnull
  @Override
  public Type exceptionType() {
    Type exceptionType = super.exceptionType();
    Preconditions.checkArgument(exceptionType != null, "Exception type is required");
    return exceptionType;
  }

  public Class<? extends SECheck> check() {
    return check;
  }

  @Override
  public String toString() {
    Type exceptionType = exceptionType();
    Preconditions.checkState(exceptionType != null);
    return String.format("{params: %s, exceptional (%s), check: %s}",
      parametersConstraints.stream().map(pMap -> MethodYield.pmapToStream(pMap).map(Constraint::toString).collect(Collectors.toList())).collect(Collectors.toList()),
      exceptionType.fullyQualifiedName(), check.getSimpleName());
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(11, 1297)
      .appendSuper(super.hashCode())
      .append(check)
      .hashCode();
  }

  @Override
  public Set<List<Location>> flow(List<Integer> parameterIndices, List<Class<? extends Constraint>> domains) {
    return ImmutableSet.of();
  }

  public Set<List<JavaFileScannerContext.Location>> exceptionFlows() {
    Set<List<JavaFileScannerContext.Location>> flows = FlowComputation.flow(node, svCausingException, domains(node.programState.getConstraints(svCausingException)));
    Tree syntaxTree = node.programPoint.syntaxTree();
    ImmutableSet.Builder<List<JavaFileScannerContext.Location>> flowBuilder = ImmutableSet.builder();

    for (List<JavaFileScannerContext.Location> flow : flows) {
      List<JavaFileScannerContext.Location> newFlow = ImmutableList.<JavaFileScannerContext.Location>builder()
        .add(new JavaFileScannerContext.Location("'" + exceptionType().name() + "' is thrown here.", syntaxTree))
        .addAll(flow)
        .build();
      flowBuilder.add(newFlow);
    }

    return flowBuilder.build();
  }

  private static List<Class<? extends Constraint>> domains(PMap<Class<? extends Constraint>, Constraint> constraints) {
    List<Class<? extends Constraint>> domains = new ArrayList<>();
    constraints.forEach((d, c) -> domains.add(d));
    return domains;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ExceptionalCheckBasedYield other = (ExceptionalCheckBasedYield) obj;
    return new EqualsBuilder()
      .appendSuper(super.equals(obj))
      .append(svCausingException, other.svCausingException)
      .append(check, other.check)
      .isEquals();
  }

  @Override
  public boolean generatedByCheck(SECheck check) {
    return this.check == check.getClass();
  }
}
