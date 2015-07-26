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
package org.sonar.java.symexec;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.symexec.ExecutionState.RELATION_RELATION_MAP;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;
import static org.sonar.java.symexec.SymbolicRelation.EQUAL_TO;
import static org.sonar.java.symexec.SymbolicRelation.GREATER_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.GREATER_THAN;
import static org.sonar.java.symexec.SymbolicRelation.LESS_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.LESS_THAN;
import static org.sonar.java.symexec.SymbolicRelation.NOT_EQUAL;

public class ExecutionStateTest {

  @Test
  public void test_relation_mapping() {
    assertThat(RELATION_RELATION_MAP.get(EQUAL_TO, EQUAL_TO)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(EQUAL_TO, GREATER_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(EQUAL_TO, GREATER_THAN)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(EQUAL_TO, LESS_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(EQUAL_TO, LESS_THAN)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(EQUAL_TO, NOT_EQUAL)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(EQUAL_TO, SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(RELATION_RELATION_MAP.get(GREATER_EQUAL, EQUAL_TO)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(GREATER_EQUAL, GREATER_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_EQUAL, GREATER_THAN)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(GREATER_EQUAL, LESS_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(GREATER_EQUAL, LESS_THAN)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_EQUAL, NOT_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(GREATER_EQUAL, SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(RELATION_RELATION_MAP.get(GREATER_THAN, EQUAL_TO)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_THAN, GREATER_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_THAN, GREATER_THAN)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_THAN, LESS_EQUAL)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_THAN, LESS_THAN)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_THAN, NOT_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(GREATER_THAN, SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(RELATION_RELATION_MAP.get(LESS_EQUAL, EQUAL_TO)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(LESS_EQUAL, GREATER_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(LESS_EQUAL, GREATER_THAN)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(LESS_EQUAL, LESS_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(LESS_EQUAL, LESS_THAN)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(LESS_EQUAL, NOT_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(LESS_EQUAL, SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(RELATION_RELATION_MAP.get(LESS_THAN, EQUAL_TO)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(LESS_THAN, GREATER_EQUAL)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(LESS_THAN, GREATER_THAN)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(LESS_THAN, LESS_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(LESS_THAN, LESS_THAN)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(LESS_THAN, NOT_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(LESS_THAN, SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(RELATION_RELATION_MAP.get(NOT_EQUAL, EQUAL_TO)).isSameAs(FALSE);
    assertThat(RELATION_RELATION_MAP.get(NOT_EQUAL, GREATER_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(NOT_EQUAL, GREATER_THAN)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(NOT_EQUAL, LESS_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(NOT_EQUAL, LESS_THAN)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(NOT_EQUAL, NOT_EQUAL)).isSameAs(TRUE);
    assertThat(RELATION_RELATION_MAP.get(NOT_EQUAL, SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN, EQUAL_TO)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN, GREATER_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN, GREATER_THAN)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN, LESS_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN, LESS_THAN)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN, NOT_EQUAL)).isSameAs(UNKNOWN);
    assertThat(RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN, SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN);
  }

  @Test
  public void test_get_set_boolean_constraint() {
    ExecutionState state = new ExecutionState();

    Symbol.VariableSymbol fieldSymbol = mock(Symbol.VariableSymbol.class);
    when(fieldSymbol.isVariableSymbol()).thenReturn(true);
    SymbolicValue.SymbolicVariableValue fieldValue = new SymbolicValue.SymbolicVariableValue(fieldSymbol);

    // constraint for a variable is unknown by default and can be set.
    assertThat(state.getBooleanConstraint(fieldValue)).isSameAs(UNKNOWN);
    state.setBooleanConstraint(fieldValue, FALSE);
    assertThat(state.getBooleanConstraint(fieldValue)).isSameAs(FALSE);

    // constraint for a variable must be queried in the parent state.
    ExecutionState nestedState = new ExecutionState(state);
    assertThat(nestedState.getBooleanConstraint(fieldValue)).isSameAs(FALSE);

    // constraint for a variable must shadow constraint from the parent state.
    nestedState.setBooleanConstraint(fieldValue, TRUE);
    assertThat(state.getBooleanConstraint(fieldValue)).isSameAs(FALSE);
    assertThat(nestedState.getBooleanConstraint(fieldValue)).isSameAs(TRUE);

    // state.setBooleanConstraint must return state
    assertThat(state.setBooleanConstraint(fieldValue, UNKNOWN)).isSameAs(state);
  }

  @Test
  public void test_get_set_relation() {
    SymbolicValue leftValue = new SymbolicValue.SymbolicVariableValue(mock(Symbol.VariableSymbol.class));
    SymbolicValue rightValue = new SymbolicValue.SymbolicVariableValue(mock(Symbol.VariableSymbol.class));

    ExecutionState state = new ExecutionState();

    // unregistered relations should evaluate to UNKNOWN.
    assertThat(state.getRelation(leftValue, rightValue)).isSameAs(SymbolicRelation.UNKNOWN);
    assertThat(state.evaluateRelation(leftValue, SymbolicRelation.UNKNOWN, rightValue)).isSameAs(UNKNOWN);

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

    // state.setRelation must return state
    assertThat(state.setRelation(leftValue, SymbolicRelation.UNKNOWN, rightValue)).isSameAs(state);
  }

  @Test
  public void test_evaluate_relation() {
    // if one of the relation is unknown the result in unknown, regardless of the second relation.
    for (Map.Entry<SymbolicRelation, SymbolicBooleanConstraint> entry : ExecutionState.RELATION_RELATION_MAP.column(SymbolicRelation.UNKNOWN).entrySet()) {
      assertThat(entry.getValue()).isSameAs(UNKNOWN);
    }
    for (Map.Entry<SymbolicRelation, SymbolicBooleanConstraint> entry : ExecutionState.RELATION_RELATION_MAP.row(SymbolicRelation.UNKNOWN).entrySet()) {
      assertThat(entry.getValue()).isSameAs(UNKNOWN);
    }
  }

  @Test
  public void test_merge_relations() {
    SymbolicValue symbol11 = new SymbolicValue.SymbolicVariableValue(mockLocalVariable());
    SymbolicValue symbol12 = new SymbolicValue.SymbolicVariableValue(mockLocalVariable());

    ExecutionState parentState = new ExecutionState();
    ExecutionState state = new ExecutionState(parentState);
    ExecutionState childState1 = new ExecutionState(state);
    ExecutionState childState21 = new ExecutionState(state);
    ExecutionState childState22 = new ExecutionState(new ExecutionState(childState21));
    ExecutionState childState31 = new ExecutionState(state);
    ExecutionState childState32 = new ExecutionState(new ExecutionState(childState31));

    parentState.setRelation(symbol11, SymbolicRelation.GREATER_THAN, symbol12);
    childState1.setRelation(symbol11, SymbolicRelation.GREATER_THAN, symbol12);
    childState21.setRelation(symbol11, SymbolicRelation.GREATER_THAN, symbol12);
    childState32.setRelation(symbol11, SymbolicRelation.GREATER_THAN, symbol12);
    state.mergeRelations(ImmutableList.of(childState1, childState22, childState32));
    assertThat(state.relations.get(symbol11, symbol12)).isNull();
    assertThat(state.getRelation(symbol11, symbol12)).isEqualTo(SymbolicRelation.GREATER_THAN);
    assertThat(state.relations.get(symbol12, symbol11)).isNull();
    assertThat(state.getRelation(symbol12, symbol11)).isEqualTo(SymbolicRelation.LESS_THAN);

    parentState.setRelation(symbol11, SymbolicRelation.UNKNOWN, symbol12);
    childState1.setRelation(symbol11, SymbolicRelation.LESS_THAN, symbol12);
    childState21.setRelation(symbol11, SymbolicRelation.LESS_EQUAL, symbol12);
    childState32.setRelation(symbol11, SymbolicRelation.LESS_EQUAL, symbol12);
    state.mergeRelations(ImmutableList.of(childState1, childState22, childState32));
    assertThat(state.getRelation(symbol11, symbol12)).isEqualTo(SymbolicRelation.LESS_EQUAL);
    assertThat(state.getRelation(symbol12, symbol11)).isEqualTo(SymbolicRelation.GREATER_EQUAL);
  }

  private Symbol.VariableSymbol mockLocalVariable() {
    Symbol.TypeSymbol methodSymbol = mock(Symbol.TypeSymbol.class);
    when(methodSymbol.isMethodSymbol()).thenReturn(true);

    Symbol.VariableSymbol variableSymbol = mock(Symbol.VariableSymbol.class);
    when(variableSymbol.isVariableSymbol()).thenReturn(true);
    when(variableSymbol.owner()).thenReturn(methodSymbol);
    return variableSymbol;
  }

}
