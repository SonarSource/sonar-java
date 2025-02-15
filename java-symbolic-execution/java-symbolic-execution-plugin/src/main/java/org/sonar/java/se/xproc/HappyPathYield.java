/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se.xproc;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

public class HappyPathYield extends MethodYield {

  private int resultIndex;
  @Nullable
  private ConstraintsByDomain resultConstraint;

  public HappyPathYield(MethodBehavior behavior) {
    super(behavior);
    this.resultIndex = -1;
    this.resultConstraint = null;
  }

  public HappyPathYield(ExplodedGraph.Node node, MethodBehavior behavior) {
    super(node, behavior);
    this.resultIndex = -1;
    this.resultConstraint = null;
  }

  @Override
  public Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
    Supplier<SymbolicValue> svSupplier) {
    Stream<ProgramState> results = parametersAfterInvocation(invocationArguments, invocationTypes, programState);

    // applied all constraints from parameters, stack return value
    SymbolicValue sv;
    if (resultIndex < 0 || resultIndex == invocationArguments.size()) {
      // if returnIndex is the size of invocationArguments : returning vararg parameter on a call with no elements specified
      sv = svSupplier.get();
    } else {
      // returned SV is the same as one of the arguments.
      sv = invocationArguments.get(resultIndex);
    }
    // sv can be null if method is void
    if (sv != null) {
      results = results.map(s -> s.stackValue(sv));
      if (resultConstraint != null) {
        results = results.map(s -> s.addConstraints(sv, resultConstraint));
      }
    }
    return results.distinct();
  }

  public void setResult(int resultIndex, @Nullable ConstraintsByDomain resultConstraint) {
    this.resultIndex = resultIndex;
    this.resultConstraint = resultConstraint;
  }

  @CheckForNull
  public ConstraintsByDomain resultConstraint() {
    return resultConstraint;
  }

  public int resultIndex() {
    return resultIndex;
  }

  @Override
  public String toString() {
    return String.format("{params: %s, result: %s (%d)}",
      parametersConstraints.stream().map(constraints -> constraints.stream().map(Constraint::toString).toList()).toList(),
      resultConstraint, resultIndex);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(5, 1293)
      .appendSuper(super.hashCode())
      .append(resultIndex)
      .append(resultConstraint)
      .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    HappyPathYield other = (HappyPathYield) obj;
    return new EqualsBuilder()
      .appendSuper(super.equals(obj))
      .append(resultIndex, other.resultIndex)
      .append(resultConstraint, other.resultConstraint)
      .isEquals();
  }

}
