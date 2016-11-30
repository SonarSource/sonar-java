/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodYield {
  private final boolean varArgs;
  Constraint[] parametersConstraints;
  int resultIndex;
  @Nullable
  Constraint resultConstraint;
  boolean exception;

  public MethodYield(int arity, boolean varArgs) {
    this.parametersConstraints = new Constraint[arity];
    this.varArgs = varArgs;
    this.resultIndex = -1;
    this.resultConstraint = null;
    this.exception = false;
  }

  @Override
  public String toString() {
    return "{params: " + Arrays.toString(parametersConstraints) + ", result: " + resultConstraint + " (" + resultIndex + "), exceptional: " + exception + "}";
  }

  public Collection<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
    Supplier<SymbolicValue> svSupplier) {
    Set<ProgramState> results = new LinkedHashSet<>();
    for (int index = 0; index < invocationArguments.size(); index++) {
      SymbolicValue invokedArg = invocationArguments.get(index);
      Constraint constraint = null;
      if (!varArgs || varArgsCalledWithOneArrayParameter(index, invocationTypes)) {
        constraint = parametersConstraints[index];
      } else {
        // no constraints are assigned on parameters of varargs methods when called with multiple arguments
      }
      if (constraint == null) {
        // no constraint on this parameter, let's try next one.
        continue;
      }

      Set<ProgramState> programStates = programStatesForConstraint(results.isEmpty() ? Lists.newArrayList(programState) : results, invokedArg, constraint);
      if (programStates.isEmpty()) {
        // constraint can't be satisfied, no need to process things further, this yield is not applicable.
        // TODO there might be some issue to report in this case.
        return programStates;
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
    return stateStream.collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private boolean varArgsCalledWithOneArrayParameter(int index, List<Type> types) {
    boolean isLastArgument = index == parametersConstraints.length - 1;
    boolean singleVariadicArg = parametersConstraints.length == types.size();
    return isLastArgument && singleVariadicArg && (types.get(index).isArray() || types.get(index).is("<nulltype>"));
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
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(parametersConstraints);
    result = prime * result + ((resultConstraint == null) ? 0 : resultConstraint.hashCode());
    result = prime * result + resultIndex;
    return result;
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
    if (!Arrays.equals(parametersConstraints, other.parametersConstraints)
      || exception != other.exception
      || resultIndex != other.resultIndex) {
      return false;
    }
    if (resultConstraint != null) {
      return resultConstraint.equals(other.resultConstraint);
    }
    return other.resultConstraint == null;
  }
}
