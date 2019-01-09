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

import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;

import javax.annotation.Nullable;

public interface Constraint {
  /**
   * @return String representation of value encoded by constraint for purpose of flow message
   */
  default String valueAsString() {
    return "";
  }

  /**
   * @return true if value represented by this SV is precisely known.
   */
  default boolean hasPreciseValue() {
    return false;
  }

  @Nullable
  default Constraint inverse() {
    return null;
  }

  default boolean isValidWith(@Nullable Constraint constraint) {
    return true;
  }

  /**
   * Return constraint which should be applied to the rhs of the relation,
   * if this constraint is set on the lhs of the relation.
   *
   * @param kind kind of relation over which constraint is copied
   * @return constraint to be set on rhs, null if no constraint should be set
   */
  @Nullable
  default Constraint copyOver(RelationalSymbolicValue.Kind kind) {
    switch (kind) {
      case EQUAL:
      case METHOD_EQUALS:
        return this;
      default:
        return inverse();
    }
  }
}
