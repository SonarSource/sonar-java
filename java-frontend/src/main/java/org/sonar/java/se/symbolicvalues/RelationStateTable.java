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

  static RelationState solveRelation(Kind given, Kind when) {
    RelationState relationState = get(given, when);
    if (relationState.isDetermined()) {
      return relationState;
    }
    return get(given, when.inverse()).invert();
  }
}
