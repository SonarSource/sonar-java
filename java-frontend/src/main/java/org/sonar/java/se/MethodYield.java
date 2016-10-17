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

import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodYield {
  Constraint[] parametersConstraints;
  int resultIndex;
  @Nullable
  Constraint resultConstraint;
  boolean exception;

  public MethodYield(int arity) {
    this.parametersConstraints = new Constraint[arity];
    this.resultIndex = -1;
    this.resultConstraint = null;
    this.exception = false;
  }

  @Override
  public String toString() {
    return "{params: " + Arrays.toString(parametersConstraints) + ", result: " + resultConstraint + " (" + resultIndex + "), exceptional: " + exception + "}";
  }

  public List<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, ProgramState programState, Supplier<SymbolicValue> svSupplier) {
    List<ProgramState> results = new ArrayList<>();
    results.add(programState);
    for (int index = 0; index < invocationArguments.size(); index++) {
      // FIXME : varargs method should be handled
      SymbolicValue invokedArg = invocationArguments.get(index);
      Constraint constraint = parametersConstraints[Math.min(index, parametersConstraints.length - 1)];
      if (constraint == null) {
        // no constraint on this parameter, let's try next one.
        continue;
      }

      List<ProgramState> programStates = new ArrayList<>();
      if (constraint instanceof ObjectConstraint) {
        for (ProgramState state : results) {
          programStates.addAll(invokedArg.setConstraint(state, (ObjectConstraint) constraint));
        }
      } else if (constraint instanceof BooleanConstraint) {
        for (ProgramState state : results) {
          programStates.addAll(invokedArg.setConstraint(state, (BooleanConstraint) constraint));
        }
      }
      if (programStates.isEmpty()) {
        // constraint can't be satisfied, no need to process things further, this yield is not applicable.
        // TODO there might be some issue to report in this case.
        return programStates;
      }
      results.addAll(programStates);
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
    return stateStream.collect(Collectors.toList());
  }

  public boolean similarYield(MethodYield other) {
    if (exception != other.exception) {
      return false;
    }
    if (resultIndex != other.resultIndex || !sameConstraintWithSameStatus(resultConstraint, other.resultConstraint)) {
      return false;
    }
    if (parametersConstraints.length != other.parametersConstraints.length) {
      return false;
    }
    for (int i = 0; i < parametersConstraints.length; i++) {
      if (!sameConstraintWithSameStatus(parametersConstraints[i], other.parametersConstraints[i])) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameConstraintWithSameStatus(Constraint a, Constraint b) {
    if (a instanceof ObjectConstraint && b instanceof ObjectConstraint) {
      ObjectConstraint aObject = (ObjectConstraint) a;
      ObjectConstraint bObject = (ObjectConstraint) b;
      if (aObject.isNull() && bObject.isNull()) {
        return true;
      }
      if (!aObject.isNull() && !bObject.isNull()) {
        return aObject.sameStatus(bObject);
      }
    }
    return a == b;
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
