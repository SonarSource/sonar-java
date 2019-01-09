/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
      case LESS_THAN:
      case GREATER_THAN_OR_EQUAL:
        return null;
      case EQUAL:
      case METHOD_EQUALS:
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
