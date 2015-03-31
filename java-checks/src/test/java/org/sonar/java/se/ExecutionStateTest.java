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
package org.sonar.java.se;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.se.SymbolicValue.UNKNOWN_VALUE;

public class ExecutionStateTest {

  @Test
  public void test_get_symbolic_value() {
    LiteralTree literalTree = new LiteralTreeImpl(Tree.Kind.NULL_LITERAL, Mockito.mock(InternalSyntaxToken.class));
    assertThat(new ExecutionState().getSymbolicValue(literalTree)).isSameAs(UNKNOWN_VALUE);

    Symbol.TypeSymbol ownerSymbol = mock(Symbol.TypeSymbol.class);

    Symbol.VariableSymbol fieldSymbol = mock(Symbol.VariableSymbol.class);
    when(fieldSymbol.isVariableSymbol()).thenReturn(true);
    when(fieldSymbol.owner()).thenReturn(ownerSymbol);

    IdentifierTreeImpl fieldTree = new IdentifierTreeImpl(Mockito.mock(InternalSyntaxToken.class));
    fieldTree.setSymbol(fieldSymbol);

    // symbolic value for a class field is unknown
    when(ownerSymbol.isMethodSymbol()).thenReturn(false);
    assertThat(new ExecutionState().getSymbolicValue(fieldTree)).isSameAs(UNKNOWN_VALUE);

    // symbolic value for a local variable is not unknown
    ExecutionState state = new ExecutionState();
    when(ownerSymbol.isMethodSymbol()).thenReturn(true);
    SymbolicValue value = state.getSymbolicValue(fieldTree);
    assertThat(value).isNotSameAs(UNKNOWN_VALUE);
    assertThat(state.getSymbolicValue(fieldTree)).isSameAs(value);

    // symbolic value for a local variable in a nested state is not unknown
    ExecutionState nestedState = new ExecutionState(state);
    assertThat(nestedState.getSymbolicValue(fieldTree)).isSameAs(value);
  }

  @Test
  public void test_get_set_relation() {
    SymbolicValue leftValue = mock(SymbolicValue.class);
    SymbolicValue rightValue = mock(SymbolicValue.class);

    // relations containing an unknown value are not registered.
    ExecutionState state = new ExecutionState();
    state.setRelation(UNKNOWN_VALUE, SymbolicRelation.UNKNOWN, UNKNOWN_VALUE);
    assertThat(state.relations).isEmpty();
    state.setRelation(new SymbolicValue(), SymbolicRelation.UNKNOWN, UNKNOWN_VALUE);
    assertThat(state.relations).isEmpty();
    state.setRelation(UNKNOWN_VALUE, SymbolicRelation.UNKNOWN, new SymbolicValue());
    assertThat(state.relations).isEmpty();

    // unregistered relations should evaluate to UNKNOWN.
    assertThat(state.getRelation(leftValue, rightValue)).isSameAs(SymbolicRelation.UNKNOWN);

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

  @Test(expected = IllegalStateException.class)
  public void test_set_relation_invalid() {
    // relations with an unknown relational operator are not allowed.
    new ExecutionState().setRelation(new SymbolicValue(), SymbolicRelation.UNKNOWN, new SymbolicValue());
  }

  @Test
  public void test_evaluate_relation() {
    // associativity: op1 op2 === op2 op1
    for (Map.Entry<SymbolicRelation, Map<SymbolicRelation, SymbolicValue>> entry1 : ExecutionState.RELATION_RELATION_MAP.entrySet()) {
      for (Map.Entry<SymbolicRelation, SymbolicValue> entry2 : entry1.getValue().entrySet()) {
        assertThat(ExecutionState.RELATION_RELATION_MAP.get(entry2.getKey()).get(entry1.getKey())).isSameAs(entry2.getValue());
      }
    }
    // if one of the relation is unknown the result in unknown, regardless of the second relation.
    for (Map.Entry<SymbolicRelation, Map<SymbolicRelation, SymbolicValue>> entry : ExecutionState.RELATION_RELATION_MAP.entrySet()) {
      assertThat(entry.getValue().get(SymbolicRelation.UNKNOWN)).isSameAs(UNKNOWN_VALUE);
    }
    for (Map.Entry<SymbolicRelation, SymbolicValue> entry : ExecutionState.RELATION_RELATION_MAP.get(SymbolicRelation.UNKNOWN).entrySet()) {
      assertThat(entry.getValue()).isSameAs(UNKNOWN_VALUE);
    }
  }

}
