/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.symexec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ExecutionState {

  // FIXME(merciesa): find a better name...
  @VisibleForTesting
  static final Map<SymbolicRelation, Map<SymbolicRelation, SymbolicBooleanConstraint>> RELATION_RELATION_MAP = ImmutableMap
    .<SymbolicRelation, Map<SymbolicRelation, SymbolicBooleanConstraint>>builder()
    .put(SymbolicRelation.EQUAL_TO, ImmutableMap.<SymbolicRelation, SymbolicBooleanConstraint>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicBooleanConstraint.TRUE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_THAN, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_THAN, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.UNKNOWN, SymbolicBooleanConstraint.UNKNOWN)
      .build())
    .put(SymbolicRelation.GREATER_EQUAL, ImmutableMap.<SymbolicRelation, SymbolicBooleanConstraint>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicBooleanConstraint.TRUE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_THAN, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.UNKNOWN, SymbolicBooleanConstraint.UNKNOWN)
      .build())
    .put(SymbolicRelation.GREATER_THAN, ImmutableMap.<SymbolicRelation, SymbolicBooleanConstraint>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_THAN, SymbolicBooleanConstraint.TRUE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.LESS_THAN, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.UNKNOWN, SymbolicBooleanConstraint.UNKNOWN)
      .build())
    .put(SymbolicRelation.LESS_EQUAL, ImmutableMap.<SymbolicRelation, SymbolicBooleanConstraint>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_THAN, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicBooleanConstraint.TRUE)
      .put(SymbolicRelation.LESS_THAN, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.UNKNOWN, SymbolicBooleanConstraint.UNKNOWN)
      .build())
    .put(SymbolicRelation.LESS_THAN, ImmutableMap.<SymbolicRelation, SymbolicBooleanConstraint>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_THAN, SymbolicBooleanConstraint.TRUE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.UNKNOWN, SymbolicBooleanConstraint.UNKNOWN)
      .build())
    .put(SymbolicRelation.NOT_EQUAL, ImmutableMap.<SymbolicRelation, SymbolicBooleanConstraint>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicBooleanConstraint.FALSE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_THAN, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_THAN, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicBooleanConstraint.TRUE)
      .put(SymbolicRelation.UNKNOWN, SymbolicBooleanConstraint.UNKNOWN)
      .build())
    .put(SymbolicRelation.UNKNOWN, ImmutableMap.<SymbolicRelation, SymbolicBooleanConstraint>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.GREATER_THAN, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.LESS_THAN, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN)
      .put(SymbolicRelation.UNKNOWN, SymbolicBooleanConstraint.UNKNOWN)
      .build())
    .build();

  @Nullable
  final ExecutionState parentState;
  final Table<Symbol.VariableSymbol, Symbol.VariableSymbol, SymbolicRelation> relations;
  final Map<Symbol.VariableSymbol, SymbolicBooleanConstraint> variables;

  public ExecutionState() {
    this.parentState = null;
    this.relations = HashBasedTable.create();
    this.variables = new HashMap<>();
  }

  ExecutionState(ExecutionState parentState) {
    this.parentState = parentState;
    this.relations = HashBasedTable.create();
    this.variables = new HashMap<>();
  }

  @VisibleForTesting
  SymbolicRelation getRelation(Symbol.VariableSymbol leftValue, Symbol.VariableSymbol rightValue) {
    SymbolicRelation result = relations.get(leftValue, rightValue);
    return result != null ? result : parentState != null ? parentState.getRelation(leftValue, rightValue) : SymbolicRelation.UNKNOWN;
  }

  SymbolicBooleanConstraint evaluateRelation(Symbol.VariableSymbol leftValue, SymbolicRelation relation, Symbol.VariableSymbol rightValue) {
    return RELATION_RELATION_MAP.get(getRelation(leftValue, rightValue)).get(relation);
  }

  void setRelation(Symbol.VariableSymbol leftValue, SymbolicRelation relation, Symbol.VariableSymbol rightValue) {
    if (!leftValue.equals(rightValue)) {
      relations.put(leftValue, rightValue, relation);
      relations.put(rightValue, leftValue, relation.swap());
    }
  }

}
