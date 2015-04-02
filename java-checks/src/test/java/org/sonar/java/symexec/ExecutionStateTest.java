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

import com.google.common.collect.Table;
import org.junit.Test;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;

public class ExecutionStateTest {

  @Test
  public void test_get_set_relation() {
    Symbol.VariableSymbol leftValue = mock(Symbol.VariableSymbol.class);
    Symbol.VariableSymbol rightValue = mock(Symbol.VariableSymbol.class);

    ExecutionState state = new ExecutionState();

    // unregistered relations should evaluate to UNKNOWN.
    assertThat(state.getRelation(leftValue, rightValue)).isSameAs(SymbolicRelation.UNKNOWN);

    // relations cannot be set between the same symbol.
    state.setRelation(leftValue, SymbolicRelation.GREATER_EQUAL, leftValue);
    assertThat(state.relations.size()).isEqualTo(0);

    // relations should be registered (relations are registered twice).
    state.setRelation(leftValue, SymbolicRelation.GREATER_EQUAL, rightValue);
    assertThat(state.relations.size()).isEqualTo(1 * 2);
    assertThat(state.getRelation(leftValue, rightValue)).isSameAs(SymbolicRelation.GREATER_EQUAL);
    assertThat(state.getRelation(rightValue, leftValue)).isSameAs(SymbolicRelation.LESS_EQUAL);

    // relations registered in parent state should be available in nested state.
    ExecutionState nestedState = new ExecutionState(state);
    assertThat(nestedState.relations.size()).isEqualTo(0);
    assertThat(nestedState.getRelation(leftValue, rightValue)).isSameAs(SymbolicRelation.GREATER_EQUAL);
    assertThat(nestedState.getRelation(rightValue, leftValue)).isSameAs(SymbolicRelation.LESS_EQUAL);

    // relations registered in nested state should shadow constraints in parent state (relations are registered twice).
    nestedState.setRelation(leftValue, SymbolicRelation.GREATER_THAN, rightValue);
    assertThat(nestedState.relations.size()).isEqualTo(1 * 2);
    assertThat(nestedState.getRelation(leftValue, rightValue)).isSameAs(SymbolicRelation.GREATER_THAN);
    assertThat(nestedState.getRelation(rightValue, leftValue)).isSameAs(SymbolicRelation.LESS_THAN);
  }

  @Test
  public void test_evaluate_relation() {
    // associativity: op1 op2 === op2 op1
    for (Table.Cell<SymbolicRelation, SymbolicRelation, SymbolicBooleanConstraint> e : ExecutionState.RELATION_RELATION_MAP.cellSet()) {
      assertThat(ExecutionState.RELATION_RELATION_MAP.get(e.getRowKey(), e.getColumnKey())).isSameAs(ExecutionState.RELATION_RELATION_MAP.get(e.getColumnKey(), e.getRowKey()));
    }
    // if one of the relation is unknown the result in unknown, regardless of the second relation.
    for (Map.Entry<SymbolicRelation, SymbolicBooleanConstraint> entry : ExecutionState.RELATION_RELATION_MAP.column(SymbolicRelation.UNKNOWN).entrySet()) {
      assertThat(entry.getValue()).isSameAs(UNKNOWN);
    }
    for (Map.Entry<SymbolicRelation, SymbolicBooleanConstraint> entry : ExecutionState.RELATION_RELATION_MAP.row(SymbolicRelation.UNKNOWN).entrySet()) {
      assertThat(entry.getValue()).isSameAs(UNKNOWN);
    }
  }

}
