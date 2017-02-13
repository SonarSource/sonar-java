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
import com.google.common.collect.Lists;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class MethodYield {
  private final ExplodedGraph.Node node;
  private final MethodBehavior behavior;
  private Constraint[] parametersConstraints;

  public MethodYield(MethodBehavior behavior) {
    this.parametersConstraints = new Constraint[behavior.methodArity()];
    this.node = null;
    this.behavior = behavior;
  }

  public MethodYield(ExplodedGraph.Node node, MethodBehavior behavior) {
    this.parametersConstraints = new Constraint[behavior.methodArity()];
    this.node = node;
    this.behavior = behavior;
  }

  public abstract Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
    Supplier<SymbolicValue> svSupplier);

  public Stream<ProgramState> parametersAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState) {
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
    return results.stream();
  }

  @CheckForNull
  private Constraint getConstraint(int index, List<Type> invocationTypes) {
    if (!behavior.isMethodVarArgs() || applicableOnVarArgs(index, invocationTypes)) {
      return parametersConstraints[index];
    }
    return null;
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

  public Constraint[] parametersConstraints() {
    return parametersConstraints;
  }

  @CheckForNull
  public Constraint parameterConstraint(int parameterIndex) {
    return parametersConstraints[parameterIndex];
  }

  public void setParameterConstraint(int index, @Nullable Constraint constraint) {
    Preconditions.checkArgument(index < parametersConstraints.length);
    parametersConstraints[index] = constraint;
  }

  public boolean generatedByCheck(SECheck check) {
    return false;
  }

  @Override
  public abstract String toString();

  @Override
  public int hashCode() {
    return new HashCodeBuilder(7, 1291)
      .append(parametersConstraints)
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
      .isEquals();
  }

  public List<JavaFileScannerContext.Location> flow(int parameterIndex) {
    if (node == null) {
      return Lists.newArrayList();
    }
    if(parameterIndex < 0) {
      return FlowComputation.flow(node, node.programState.exitValue());
    }
    return FlowComputation.flow(node, behavior.parameters().get(parameterIndex));
  }
}
