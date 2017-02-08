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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.java.collections.PMap;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HappyPathYield extends MethodYield {

  private int resultIndex;
  @Nullable
  private PMap<Class<? extends Constraint>, Constraint> resultConstraint;

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
    if (resultIndex < 0) {
      sv = svSupplier.get();
    } else {
      // returned SV is the same as one of the arguments.
      sv = invocationArguments.get(resultIndex);
    }
    results = results.map(s -> s.stackValue(sv));
    if (resultConstraint != null) {
      results = results.map(s -> s.addConstraints(sv, resultConstraint));
    }
    return results.distinct();
  }

  public void setResult(int resultIndex, @Nullable PMap<Class<? extends Constraint>, Constraint> resultConstraint) {
    this.resultIndex = resultIndex;
    this.resultConstraint = resultConstraint;
  }

  @VisibleForTesting
  public PMap<Class<? extends Constraint>, Constraint> resultConstraint() {
    return resultConstraint;
  }

  @VisibleForTesting
  public int resultIndex() {
    return resultIndex;
  }

  @Override
  public String toString() {
    return String.format("{params: %s, result: %s (%d)}",
      parametersConstraints.stream().map(pMap -> MethodYield.pmapToStream(pMap).map(Constraint::toString).collect(Collectors.toList())).collect(Collectors.toList()),
      resultConstraint, resultIndex);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(5, 1293)
      .appendSuper(super.hashCode())
      .append(resultIndex)
      .append(resultConstraint)
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
    HappyPathYield other = (HappyPathYield) obj;
    return new EqualsBuilder()
      .appendSuper(super.equals(obj))
      .append(resultIndex, other.resultIndex)
      .append(resultConstraint, other.resultConstraint)
      .isEquals();
  }

}
