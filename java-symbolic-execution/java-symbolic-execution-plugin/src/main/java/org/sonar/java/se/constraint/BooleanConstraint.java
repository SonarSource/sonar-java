/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.se.constraint;

import javax.annotation.Nullable;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;

public enum BooleanConstraint implements Constraint {
  TRUE,
  FALSE;

  public boolean isTrue() {
    return this == TRUE;
  }

  public boolean isFalse() {
    return this == FALSE;
  }

  @Override
  public boolean hasPreciseValue() {
    return true;
  }

  @Override
  public String valueAsString() {
    if (this == TRUE) {
      return "true";
    }
    return "false";
  }

  @Nullable
  @Override
  public Constraint copyOver(RelationalSymbolicValue.Kind kind) {
    switch (kind) {
      case LESS_THAN,
        GREATER_THAN_OR_EQUAL:
        return null;
      case EQUAL,
        METHOD_EQUALS:
        return this;
      default:
        return inverse();
    }
  }

  @Override
  public boolean isValidWith(@Nullable Constraint constraint) {
    return constraint == null || this == constraint;
  }

  @Override
  public BooleanConstraint inverse() {
    if (TRUE == this) {
      return FALSE;
    }
    return TRUE;
  }
}
