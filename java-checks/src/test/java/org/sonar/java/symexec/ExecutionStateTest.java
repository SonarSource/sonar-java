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

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;

public class ExecutionStateTest {

  @Test
  public void test_get_set_boolean_constraint() {
    ExecutionState state = new ExecutionState();

    SymbolicValue value = new SymbolicValue.SymbolicInstanceValue();

    // constraint for a variable is unknown by default and can be set.
    assertThat(state.getBooleanConstraint(value)).isSameAs(UNKNOWN);
    state.setBooleanConstraint(value, FALSE);
    assertThat(state.getBooleanConstraint(value)).isSameAs(FALSE);

    // constraint for a variable must be queried in the parent state.
    ExecutionState nestedState = new ExecutionState(state);
    assertThat(nestedState.getBooleanConstraint(value)).isSameAs(FALSE);

    // constraint for a variable must shadow constraint from the parent state.
    nestedState.setBooleanConstraint(value, TRUE);
    assertThat(state.getBooleanConstraint(value)).isSameAs(FALSE);
    assertThat(nestedState.getBooleanConstraint(value)).isSameAs(TRUE);

    // state.setBooleanConstraint must return state
    assertThat(state.setBooleanConstraint(value, UNKNOWN)).isSameAs(state);
  }

  @Test
  public void test_get_set_relation() {
    SymbolicValue leftValue = new SymbolicValue.SymbolicInstanceValue();
    SymbolicValue rightValue = new SymbolicValue.SymbolicInstanceValue();

    ExecutionState state;

    state = new ExecutionState();
    assertThat(state.getRelation(leftValue, rightValue)).isSameAs(SymbolicRelation.UNKNOWN);
    assertThat(state.evaluateRelation(leftValue, SymbolicRelation.UNKNOWN, rightValue)).isSameAs(null);

    // relations cannot be set between the same symbol.
    state = new ExecutionState();
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

}
