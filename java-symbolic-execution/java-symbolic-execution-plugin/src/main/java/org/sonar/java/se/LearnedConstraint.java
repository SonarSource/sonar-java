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
package org.sonar.java.se;

import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

import java.util.Objects;

public class LearnedConstraint {
  final SymbolicValue sv;
  final Constraint constraint;

  public LearnedConstraint(SymbolicValue sv, Constraint constraint) {
    Objects.requireNonNull(constraint);
    this.sv = sv;
    this.constraint = constraint;
  }

  public SymbolicValue symbolicValue() {
    return sv;
  }

  public Constraint constraint() {
    return constraint;
  }

  @Override
  public String toString() {
    return sv + " - " + constraint;
  }
}
