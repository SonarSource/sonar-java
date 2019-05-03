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
package org.sonar.java.se.xproc;

import com.google.common.base.Preconditions;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ExplodedGraph.Node;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExceptionalCheckBasedYield extends ExceptionalYield {

  private final Class<? extends SECheck> check;
  private final SymbolicValue svCausingException;
  private final boolean isMethodVarargs;

  public ExceptionalCheckBasedYield(SymbolicValue svCausingException, String exceptionType, Class<? extends SECheck> check, Node node, MethodBehavior behavior) {
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
      ConstraintsByDomain yieldConstraint = parametersConstraints.get(index);
      ConstraintsByDomain stateConstraint = argumentConstraint(invocationArguments, programState, index);
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
    ConstraintsByDomain lastParamConstraint = parametersConstraints.get(numberParametersYield - 1);
    if (lastParamConstraint.isEmpty()) {
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
  private static ConstraintsByDomain argumentConstraint(List<SymbolicValue> invocationArguments, ProgramState programState, int index) {
    if (index < invocationArguments.size()) {
      return programState.getConstraints(invocationArguments.get(index));
    }
    return null;
  }

  @Override
  public void setExceptionType(String exceptionType) {
    throw new UnsupportedOperationException("Exception type can not be changed");
  }

  @Nonnull
  @Override
  public Type exceptionType(SemanticModel semanticModel) {
    Type exceptionType = super.exceptionType(semanticModel);
    Preconditions.checkArgument(!exceptionType.isUnknown(), "Exception type is required");
    return exceptionType;
  }

  public Class<? extends SECheck> check() {
    return check;
  }

  @Override
  public String toString() {
    return super.toString().replace('}', ',')+" check: "+check.getSimpleName()+"}";
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(11, 1297)
      .appendSuper(super.hashCode())
      .append(check)
      .hashCode();
  }

  @Override
  public Set<Flow> flow(List<Integer> parameterIndices, List<Class<? extends Constraint>> domains) {
    return Collections.emptySet();
  }

  public Set<Flow> exceptionFlows() {
    List<Class<? extends Constraint>> domains = node.programState.getConstraints(svCausingException).domains().collect(Collectors.toList());
    return FlowComputation.flow(node, svCausingException, domains);
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

  public int parameterCausingExceptionIndex() {
    return methodBehavior().parameters().indexOf(svCausingException);
  }
}
