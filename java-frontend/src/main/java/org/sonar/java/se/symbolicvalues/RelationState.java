/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.Nullable;

/**
 * This enum values are the possible returns of the method implies(SymbolicValueRelation) of class SymbolicValueRelation.
 * @see BinaryRelation#implies
 */
public enum RelationState {
  /**
   * This value means that the checked relation is fulfilled by the set of known relations
   */
  FULFILLED(true, BooleanConstraint.FALSE),
  /**
   * This value means that the checked relation is not fulfilled by the set of known relations
   */
  UNFULFILLED(true, BooleanConstraint.TRUE),
  /**
   * This value means that the checked relation is not determined by the set of known relations
   */
  UNDETERMINED(false, null);

  private final boolean determined;
  private final BooleanConstraint checkedConstraint;

  RelationState(boolean determined, @Nullable BooleanConstraint checkedConstraint) {
    this.determined = determined;
    this.checkedConstraint = checkedConstraint;
  }

  public boolean rejects(BooleanConstraint constraint) {
    return checkedConstraint != null && checkedConstraint.equals(constraint);
  }

  public boolean isDetermined() {
    return determined;
  }

}

