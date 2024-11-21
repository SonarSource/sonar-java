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
package org.sonar.java.se.constraint;

import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;

import javax.annotation.Nullable;

public enum ObjectConstraint implements Constraint {
  NULL,
  NOT_NULL;

  public boolean isNull() {
    return this == NULL;
  }

  @Override
  public boolean hasPreciseValue() {
    return this == NULL;
  }

  @Override
  public String valueAsString() {
    if (this == NULL) {
      return "null";
    }
    return "not null";
  }

  @Override
  public boolean isValidWith(@Nullable Constraint constraint) {
    return constraint == null || this == constraint;
  }

  @Nullable
  @Override
  public Constraint copyOver(RelationalSymbolicValue.Kind kind) {
    switch (kind) {
      case EQUAL,
        METHOD_EQUALS:
        return this;
      default:
        // x != NULL -> x is NOT_NULL , but if x != NOT_NULL we learn nothing about x
        return this == NULL ? NOT_NULL : null;
    }
  }

  @Override
  public Constraint inverse() {
    if(this == NULL) {
      return NOT_NULL;
    }
    return NULL;
  }
}
