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

import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MethodYield {
  private final boolean varArgs;
  Constraint[] parametersConstraints;
  int resultIndex;
  @Nullable
  Constraint resultConstraint;
  @Nullable
  Type exceptionType;
  boolean exception;

  public MethodYield(int arity, boolean varArgs) {
    this.parametersConstraints = new Constraint[arity];
    this.varArgs = varArgs;
    this.resultIndex = -1;
    this.resultConstraint = null;
    this.exception = false;
    this.exceptionType = null;
  }

  @Override
  public String toString() {
    return String.format("{params: %s, result: %s (%d), exceptional: %b%s}",
      Arrays.toString(parametersConstraints),
      resultConstraint,
      resultIndex,
      exception,
      exceptionType == null ? "" : (" (" + exceptionType + ")"));
  }

  public Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
    Supplier<SymbolicValue> svSupplier) {
    Set<ProgramState> results = new LinkedHashSet<>();
    for (int index = 0; index < invocationArguments.size(); index++) {
      Constraint constraint = getConstraint(index, invocationTypes);
      if (constraint == null) {
        // no constraint on this parameter, let's try next one.
        continue;
      }

      SymbolicValue invokedArg = invocationArguments.get(index);
      Set<ProgramState> programStates = programStatesForConstraint(results.isEmpty() ? Lists.newArrayList(programState) : results, invokedArg, constraint);
      if (programStates.isEmpty()) {
        // constraint can't be satisfied, no need to process things further, this yield is not applicable.
        // TODO there might be some issue to report in this case.
        return Stream.empty();
      }
      results = programStates;
    }

    // resulting program states can be empty if all constraints on params are null or if method has no arguments.
    // That means that this yield is still possible and we need to stack a returned SV with its eventual constraints.
    if(results.isEmpty()) {
      results.add(programState);
    }

    // applied all constraints from parameters, stack return value
    SymbolicValue sv;
    if (resultIndex < 0) {
      sv = svSupplier.get();
    } else {
      // returned SV is the same as one of the arguments.
      sv = invocationArguments.get(resultIndex);
    }
    Stream<ProgramState> stateStream = results.stream().map(s -> s.stackValue(sv));
    if (resultConstraint != null) {
      stateStream = stateStream.map(s -> s.addConstraint(sv, resultConstraint));
    }
    return stateStream.distinct();
  }

  @CheckForNull
  private Constraint getConstraint(int index, List<Type> invocationTypes) {
    if (!varArgs || applicableOnVarArgs(index, invocationTypes)) {
      return parametersConstraints[index];
    }
    return null;
  }

  /**
   * For varArgs methods, only apply the constraint on single array parameter, in order to not 
   * wrongly apply it on all the elements of the array.
   */
  private boolean applicableOnVarArgs(int index, List<Type> types) {
    if (index < parametersConstraints.length - 1) {
      // not the varArg argument
      return true;
    }
    if (parametersConstraints.length != types.size()) {
      // more than one element in the variadic part
      return false;
    }
    Type argumentType = types.get(index);
    return argumentType.isArray() || argumentType.is("<nulltype>");
  }

  private static Set<ProgramState> programStatesForConstraint(Collection<ProgramState> states, SymbolicValue invokedArg, Constraint constraint) {
    Set<ProgramState> programStates = new LinkedHashSet<>();
    if (constraint instanceof ObjectConstraint) {
      ObjectConstraint objectConstraint = (ObjectConstraint) constraint;
      states.forEach(state -> programStates.addAll(invokedArg.setConstraint(state, objectConstraint)));
    } else if (constraint instanceof BooleanConstraint) {
      BooleanConstraint booleanConstraint = (BooleanConstraint) constraint;
      states.forEach(state -> programStates.addAll(invokedArg.setConstraint(state, booleanConstraint)));
    }
    return programStates;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(7, 1291)
      .append(parametersConstraints)
      .append(varArgs)
      .append(resultIndex)
      .append(resultIndex)
      .append(resultConstraint)
      .append(exception)
      .append(exceptionType)
      .hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MethodYield other = (MethodYield) obj;
    return new EqualsBuilder()
      .append(parametersConstraints, other.parametersConstraints)
      .append(varArgs, other.varArgs)
      .append(resultIndex, other.resultIndex)
      .append(resultConstraint, other.resultConstraint)
      .append(exception, other.exception)
      .append(exceptionType, other.exceptionType)
      .isEquals();
  }
}
