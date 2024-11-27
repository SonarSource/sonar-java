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
package org.sonar.java.se.symbolicvalues;

/**
 * This enum values are the possible returns of the method implies(SymbolicValueRelation) of class SymbolicValueRelation.
 * @see RelationalSymbolicValue#implies(RelationalSymbolicValue)
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

