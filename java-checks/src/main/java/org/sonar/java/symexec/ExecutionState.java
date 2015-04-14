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
import com.google.common.collect.Table;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicRelation.EQUAL_TO;
import static org.sonar.java.symexec.SymbolicRelation.GREATER_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.GREATER_THAN;
import static org.sonar.java.symexec.SymbolicRelation.LESS_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.LESS_THAN;
import static org.sonar.java.symexec.SymbolicRelation.NOT_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.UNKNOWN;

public class ExecutionState {

  // FIXME(merciesa): find a better name...
  @VisibleForTesting
  static final Table<SymbolicRelation, SymbolicRelation, SymbolicBooleanConstraint> RELATION_RELATION_MAP = HashBasedTable.create();

  static {
    RELATION_RELATION_MAP.put(EQUAL_TO, EQUAL_TO, TRUE);
    RELATION_RELATION_MAP.put(EQUAL_TO, GREATER_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(EQUAL_TO, GREATER_THAN, FALSE);
    RELATION_RELATION_MAP.put(EQUAL_TO, LESS_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(EQUAL_TO, LESS_THAN, FALSE);
    RELATION_RELATION_MAP.put(EQUAL_TO, NOT_EQUAL, FALSE);
    RELATION_RELATION_MAP.put(EQUAL_TO, UNKNOWN, SymbolicBooleanConstraint.UNKNOWN);

    RELATION_RELATION_MAP.put(GREATER_EQUAL, EQUAL_TO, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(GREATER_EQUAL, GREATER_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(GREATER_EQUAL, GREATER_THAN, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(GREATER_EQUAL, LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(GREATER_EQUAL, LESS_THAN, FALSE);
    RELATION_RELATION_MAP.put(GREATER_EQUAL, NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(GREATER_EQUAL, UNKNOWN, SymbolicBooleanConstraint.UNKNOWN);

    RELATION_RELATION_MAP.put(GREATER_THAN, EQUAL_TO, FALSE);
    RELATION_RELATION_MAP.put(GREATER_THAN, GREATER_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(GREATER_THAN, GREATER_THAN, TRUE);
    RELATION_RELATION_MAP.put(GREATER_THAN, LESS_EQUAL, FALSE);
    RELATION_RELATION_MAP.put(GREATER_THAN, LESS_THAN, FALSE);
    RELATION_RELATION_MAP.put(GREATER_THAN, NOT_EQUAL, SymbolicBooleanConstraint.TRUE);
    RELATION_RELATION_MAP.put(GREATER_THAN, UNKNOWN, SymbolicBooleanConstraint.UNKNOWN);

    RELATION_RELATION_MAP.put(LESS_EQUAL, EQUAL_TO, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(LESS_EQUAL, GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(LESS_EQUAL, GREATER_THAN, FALSE);
    RELATION_RELATION_MAP.put(LESS_EQUAL, LESS_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(LESS_EQUAL, LESS_THAN, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(LESS_EQUAL, NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(LESS_EQUAL, UNKNOWN, SymbolicBooleanConstraint.UNKNOWN);

    RELATION_RELATION_MAP.put(LESS_THAN, EQUAL_TO, FALSE);
    RELATION_RELATION_MAP.put(LESS_THAN, GREATER_EQUAL, FALSE);
    RELATION_RELATION_MAP.put(LESS_THAN, GREATER_THAN, FALSE);
    RELATION_RELATION_MAP.put(LESS_THAN, LESS_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(LESS_THAN, LESS_THAN, TRUE);
    RELATION_RELATION_MAP.put(LESS_THAN, NOT_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(LESS_THAN, UNKNOWN, SymbolicBooleanConstraint.UNKNOWN);

    RELATION_RELATION_MAP.put(NOT_EQUAL, EQUAL_TO, FALSE);
    RELATION_RELATION_MAP.put(NOT_EQUAL, GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(NOT_EQUAL, GREATER_THAN, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(NOT_EQUAL, LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(NOT_EQUAL, LESS_THAN, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(NOT_EQUAL, NOT_EQUAL, TRUE);
    RELATION_RELATION_MAP.put(NOT_EQUAL, UNKNOWN, SymbolicBooleanConstraint.UNKNOWN);

    RELATION_RELATION_MAP.put(UNKNOWN, EQUAL_TO, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(UNKNOWN, GREATER_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(UNKNOWN, GREATER_THAN, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(UNKNOWN, LESS_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(UNKNOWN, LESS_THAN, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(UNKNOWN, NOT_EQUAL, SymbolicBooleanConstraint.UNKNOWN);
    RELATION_RELATION_MAP.put(UNKNOWN, UNKNOWN, SymbolicBooleanConstraint.UNKNOWN);
  }

  @Nullable
  @VisibleForTesting
  final ExecutionState parentState;
  @VisibleForTesting
  final Table<Symbol.VariableSymbol, Symbol.VariableSymbol, SymbolicRelation> relations;
  @VisibleForTesting
  final Map<Symbol.VariableSymbol, SymbolicBooleanConstraint> constraints;

  public ExecutionState() {
    this.parentState = null;
    this.constraints = new HashMap<>();
    this.relations = HashBasedTable.create();
  }

  ExecutionState(ExecutionState parentState) {
    this.parentState = parentState;
    this.constraints = new HashMap<>();
    this.relations = HashBasedTable.create();
  }

  void union(Iterable<ExecutionState> states) {
    unionBooleanConstraints(states);
    unionRelations(states);
  }

  @VisibleForTesting
  SymbolicRelation getRelation(Symbol.VariableSymbol leftValue, Symbol.VariableSymbol rightValue) {
    SymbolicRelation result = relations.get(leftValue, rightValue);
    return result != null ? result : parentState != null ? parentState.getRelation(leftValue, rightValue) : UNKNOWN;
  }

  SymbolicBooleanConstraint evaluateRelation(Symbol.VariableSymbol leftValue, SymbolicRelation relation, Symbol.VariableSymbol rightValue) {
    return RELATION_RELATION_MAP.get(getRelation(leftValue, rightValue), relation);
  }

  ExecutionState setRelation(Symbol.VariableSymbol leftValue, SymbolicRelation relation, Symbol.VariableSymbol rightValue) {
    if (!leftValue.equals(rightValue)) {
      relations.put(leftValue, rightValue, relation);
      relations.put(rightValue, leftValue, relation.swap());
    }
    return this;
  }

  private void unionRelations(Iterable<ExecutionState> states) {
    for (Table.Cell<Symbol.VariableSymbol, Symbol.VariableSymbol, SymbolicRelation> cell : findCommonRelationSymbols(states).cellSet()) {
      SymbolicRelation relation = null;
      for (ExecutionState state : states) {
        relation = state.getRelation(cell.getRowKey(), cell.getColumnKey()).union(relation);
      }
      if (getRelation(cell.getRowKey(), cell.getColumnKey()) != relation) {
        relations.put(cell.getRowKey(), cell.getColumnKey(), relation);
        relations.put(cell.getColumnKey(), cell.getRowKey(), relation.swap());
      }
    }
  }

  private Table<Symbol.VariableSymbol, Symbol.VariableSymbol, SymbolicRelation> findCommonRelationSymbols(Iterable<ExecutionState> states) {
    // stored value is completely meaningless since only the pair of symbols is relevant, but HashBasedTable does not accept null.
    Table<Symbol.VariableSymbol, Symbol.VariableSymbol, SymbolicRelation> result = HashBasedTable.create();
    for (ExecutionState state : states) {
      for (ExecutionState current = state; current != this; current = current.parentState) {
        for (Map.Entry<Symbol.VariableSymbol, Map<Symbol.VariableSymbol, SymbolicRelation>> leftEntry : current.relations.rowMap().entrySet()) {
          for (Symbol.VariableSymbol rightSymbol : leftEntry.getValue().keySet()) {
            result.put(leftEntry.getKey(), rightSymbol, UNKNOWN);
          }
        }
        for (Map.Entry<Symbol.VariableSymbol, Map<Symbol.VariableSymbol, SymbolicRelation>> rightEntry : current.relations.columnMap().entrySet()) {
          for (Symbol.VariableSymbol leftSymbol : rightEntry.getValue().keySet()) {
            result.put(leftSymbol, rightEntry.getKey(), UNKNOWN);
          }
        }
      }
    }
    return result;
  }

  SymbolicBooleanConstraint getBooleanConstraint(Symbol.VariableSymbol symbol) {
    for (ExecutionState state = this; state != null; state = state.parentState) {
      SymbolicBooleanConstraint result = state.constraints.get(symbol);
      if (result != null) {
        return result;
      }
    }
    return SymbolicBooleanConstraint.UNKNOWN;
  }

  ExecutionState setBooleanConstraint(Symbol.VariableSymbol symbol, SymbolicBooleanConstraint constraint) {
    if (symbol.owner().isMethodSymbol()) {
      constraints.put(symbol, constraint);
    }
    return this;
  }

  private void unionBooleanConstraints(Iterable<ExecutionState> states) {
    for (Symbol.VariableSymbol symbolToMerge : findCommonBooleanSymbols(states)) {
      SymbolicBooleanConstraint constraint = null;
      for (ExecutionState state : states) {
        constraint = state.getBooleanConstraint(symbolToMerge).union(constraint);
      }
      if (getBooleanConstraint(symbolToMerge) != constraint) {
        setBooleanConstraint(symbolToMerge, constraint);
      }
    }
  }

  private Set<Symbol.VariableSymbol> findCommonBooleanSymbols(Iterable<ExecutionState> states) {
    Set<Symbol.VariableSymbol> result = new HashSet<>();
    for (ExecutionState state : states) {
      for (ExecutionState current = state; current != this; current = current.parentState) {
        result.addAll(state.constraints.keySet());
      }
    }
    return result;
  }

}
