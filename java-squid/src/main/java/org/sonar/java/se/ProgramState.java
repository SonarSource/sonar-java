/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.se;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.Map;

public class ProgramState {

  public static final ProgramState EMPTY_STATE = new ProgramState(Maps.<Symbol, SymbolicValue>newHashMap(),
    /* Empty state knows that null literal is null */
    ImmutableMap.<SymbolicValue, Object>builder()
        .put(SymbolicValue.NULL_LITERAL, ConstraintManager.NullConstraint.NULL)
        .put(SymbolicValue.TRUE_LITERAL, ConstraintManager.BooleanConstraint.TRUE)
        .put(SymbolicValue.FALSE_LITERAL, ConstraintManager.BooleanConstraint.FALSE)
        .build());
    Map<Symbol, SymbolicValue> values;
    Map<SymbolicValue, Object> constraints;

  public ProgramState(Map<Symbol, SymbolicValue> values, Map<SymbolicValue, Object> constraints) {
    this.values = ImmutableMap.copyOf(values);
    this.constraints = ImmutableMap.copyOf(constraints);
  }

  @Override
  public String toString() {
    return "{" + values.toString() + "}  {" + constraints.toString() + "}";
  }

}
