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
package org.sonar.java.se.symbolicvalues;

import org.sonar.java.se.constraint.BooleanConstraint;

/**
 * This enum values are the possible returns of the method implies(SymbolicValueRelation) of class SymbolicValueRelation.
 * @see BinaryRelation#implies
 */
enum RelationState {
  /**
   * This value means that the checked relation is fulfilled by the set of known relations
   */
  FULFILLED,
  /**
   * This value means that the checked relation is not fulfilled by the set of known relations
   */
  UNFULFILLED,
  /**
   * This value means that the checked relation is not determined by the set of known relations
   */
  UNDETERMINED;

  public boolean rejects(BooleanConstraint constraint) {
    if (this == FULFILLED) {
      return constraint == BooleanConstraint.FALSE;
    }
    if (this == UNFULFILLED) {
      return constraint == BooleanConstraint.TRUE;
    }
    return false;
  }

  public boolean isDetermined() {
    return this != UNDETERMINED;
  }

  RelationState invert() {
    if (this == FULFILLED) {
      return UNFULFILLED;
    }
    if (this == UNFULFILLED) {
      return FULFILLED;
    }
    return UNDETERMINED;
  }

}

