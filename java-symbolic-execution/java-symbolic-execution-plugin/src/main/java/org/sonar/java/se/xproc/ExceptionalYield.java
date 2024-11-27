/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.sonar.java.model.SESymbols;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Type;

public class ExceptionalYield extends MethodYield {

  @Nullable
  private String exceptionType;

  public ExceptionalYield(MethodBehavior behavior) {
    super(behavior);
    this.exceptionType = null;
  }

  public ExceptionalYield(ExplodedGraph.Node node, MethodBehavior behavior) {
    super(node, behavior);
    this.exceptionType = null;
  }

  @Override
  public Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
    Supplier<SymbolicValue> svSupplier) {
    return parametersAfterInvocation(invocationArguments, invocationTypes, programState)
      .map(s -> s.stackValue(svSupplier.get()))
      .distinct();
  }

  public void setExceptionType(@Nullable String exceptionType) {
    this.exceptionType = exceptionType;
  }

  @Nullable
  public String getExceptionType() {
    return exceptionType;
  }

  public Type exceptionType(Sema semanticModel) {
    if (exceptionType == null) {
      return SESymbols.unknownType;
    }
    Type type = semanticModel.getClassType(this.exceptionType);
    return type == null ? SESymbols.unknownType : type;
  }

  @Override
  public String toString() {
    return String.format("{params: %s, exceptional%s}",
      parametersConstraints.stream().map(constraints -> constraints.stream().map(Constraint::toString).toList()).toList(),
      exceptionType == null ? "" : (" (" + exceptionType + ")"));
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(3, 1295)
      .appendSuper(super.hashCode())
      .append(exceptionType)
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
    ExceptionalYield other = (ExceptionalYield) obj;
    return new EqualsBuilder()
      .appendSuper(super.equals(obj))
      .append(exceptionType, other.exceptionType)
      .isEquals();
  }

}
