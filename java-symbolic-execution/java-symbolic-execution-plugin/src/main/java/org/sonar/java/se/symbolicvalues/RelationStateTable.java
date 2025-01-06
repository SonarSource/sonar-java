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
package org.sonar.java.se.symbolicvalues;

import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind;

import java.util.EnumMap;
import java.util.Map;

import static org.sonar.java.se.symbolicvalues.RelationState.FULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationState.UNDETERMINED;
import static org.sonar.java.se.symbolicvalues.RelationState.UNFULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.METHOD_EQUALS;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_METHOD_EQUALS;

class RelationStateTable {

  private static final Map<Kind, Map<Kind, RelationState>> table = new EnumMap<>(Kind.class);

  private RelationStateTable() {
    // utility class
  }

  static  {
    set(EQUAL, LESS_THAN, UNFULFILLED);
    set(EQUAL, METHOD_EQUALS, FULFILLED);

    set(LESS_THAN, EQUAL, UNFULFILLED);
    set(LESS_THAN, LESS_THAN, UNFULFILLED);
    set(LESS_THAN, METHOD_EQUALS, UNFULFILLED);

    set(METHOD_EQUALS, LESS_THAN, UNFULFILLED);
    set(NOT_METHOD_EQUALS, EQUAL, UNFULFILLED);
  }

  private static void set(Kind op1, Kind op2, RelationState state) {
    Map<Kind, RelationState> op1Map = table.computeIfAbsent(op1, kind -> new EnumMap<>(Kind.class));
    RelationState relationState = op1Map.get(op2);
    if (relationState != null) {
      throw new IllegalStateException("Value already present!");
    }
    op1Map.put(op2, state);
  }

  private static RelationState get(Kind given, Kind when) {
    Map<Kind, RelationState> givenMap = table.get(given);
    if (givenMap == null) {
      return UNDETERMINED;
    }
    return givenMap.getOrDefault(when, UNDETERMINED);
  }

  /**
   * Solve the state of {@code toSolve} relation given that {@code known} relation is fulfilled (satisfied).
   *
   * It is assumed that relations share operands and that certain combinations of operands
   * were already eliminated in {@link RelationalSymbolicValue#implies(RelationalSymbolicValue)}
   *
   * @param known relation we know is fulfilled
   * @param toSolve relation of which state we are trying to determine
   * @return state of the toSolve relation
   */
  static RelationState solveRelation(Kind known, Kind toSolve) {
    RelationState relationState = get(known, toSolve);
    if (relationState.isDetermined()) {
      return relationState;
    }
    return get(known, toSolve.inverse()).invert();
  }
}
