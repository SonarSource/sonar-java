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
package org.sonar.java.se;

import com.google.common.collect.ImmutableList;
import junit.framework.Assert;
import org.junit.Test;

import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.Symbols;
import org.sonar.java.se.ProgramState.Pop;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ProgramStateTest {

  @Test
  public void testing_equals() {
    SymbolicValue sv1 = new SymbolicValue(1);
    ProgramState state = ProgramState.EMPTY_STATE.addConstraint(sv1, ObjectConstraint.NOT_NULL);
    assertThat(state.equals(null)).isFalse();
    assertThat(state.equals(new String())).isFalse();
    ProgramState state2 = ProgramState.EMPTY_STATE.addConstraint(sv1, ObjectConstraint.NOT_NULL);
    assertThat(state.equals(state2)).isTrue();
  }

  @Test
  public void testStackUnstack() {
    SymbolicValue sv1 = new SymbolicValue(1);
    ProgramState state = ProgramState.EMPTY_STATE.stackValue(sv1);
    assertThat(state.peekValue()).isSameAs(sv1);
    SymbolicValue sv2 = new SymbolicValue(2);
    state = state.stackValue(sv2);
    List<SymbolicValue> values = state.peekValues(2);
    assertThat(values).hasSize(2).containsSequence(sv2, sv1);
    try {
      state.peekValues(3);
      Assert.fail("Able to retrieve more values than there are actually on the stack!");
    } catch (IllegalStateException e) {
      // Expected behavior
    }
    Pop unstack = state.unstackValue(1);
    state = unstack.state;
    values = unstack.values;
    assertThat(values).hasSize(1);
    assertThat(values.get(0)).isSameAs(sv2);
    assertThat(state.peekValue()).isSameAs(sv1);
  }

  @Test
  public void testToString() {
    SymbolicValue sv3 = new SymbolicValue(3);
    ProgramState state = ProgramState.EMPTY_STATE.stackValue(sv3);
    Symbol variable = new JavaSymbol.VariableJavaSymbol(0, "x", new JavaSymbol(JavaSymbol.TYP, 0, "A", Symbols.unknownSymbol));
    SymbolicValue sv4 = new SymbolicValue(4);
    state = state.put(variable, sv4);
    SymbolicValue sv5 = new SymbolicValue(5);
    state = state.stackValue(sv5);
    state.lastEvaluated = variable;
    // FIXME to string is not really nice by displaying classes and order is not guaranteed.
    assertThat(state.toString()).contains("{ A#x->SV_4}  { SV_0_NULL-> ");
      //.isEqualTo("{ A#x->SV_4}  { SV_0_NULL-> class org.sonar.java.se.constraint.ObjectConstraint->NULL SV_1_TRUE-> class org.sonar.java.se.constraint.BooleanConstraint->TRUE class org.sonar.java.se.constraint.ObjectConstraint->NOT_NULL SV_2_FALSE-> class org.sonar.java.se.constraint.BooleanConstraint->FALSE class org.sonar.java.se.constraint.ObjectConstraint->NOT_NULL} { [SV_5, SV_3] } { A#x } ");
  }

  @Test
  public void testAddingSameConstraintTwice() {
    ProgramState state = ProgramState.EMPTY_STATE;
    SymbolicValue sv3 = new SymbolicValue(3);
    assertThat(state.getConstraint(sv3, ObjectConstraint.class)).isNull();
    state = state.addConstraint(sv3, ObjectConstraint.NOT_NULL);
    assertThat(state.getConstraint(sv3, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    ProgramState next = state.addConstraint(sv3, ObjectConstraint.NOT_NULL);
    assertThat(next).isSameAs(state);
  }

  @Test
  public void test_learned_constraint() {
    ProgramState parent = ProgramState.EMPTY_STATE;
    ProgramState child = ProgramState.EMPTY_STATE;
    assertThat(child.learnedConstraints(parent)).isEmpty();
    SymbolicValue sv = new SymbolicValue(1);
    child = child.addConstraint(sv, ObjectConstraint.NULL);
    Set<LearnedConstraint> learnedConstraints = child.learnedConstraints(parent);
    assertThat(learnedConstraints).hasSize(1);
    LearnedConstraint lc = learnedConstraints.iterator().next();
    assertThat(lc.symbolicValue()).isEqualTo(sv);
    assertThat(lc.constraint()).isEqualTo(ObjectConstraint.NULL);
  }

  @Test
  public void test_learned_constraint_binary_SV() {
    SymbolicValue sv1 = new SymbolicValue(1);
    SymbolicValue sv2 = new SymbolicValue(2);
    RelationalSymbolicValue relation = new RelationalSymbolicValue(3, RelationalSymbolicValue.Kind.EQUAL);
    relation.computedFrom(ImmutableList.of(sv1, sv2));
    ProgramState parent = ProgramState.EMPTY_STATE;
    ProgramState child = ProgramState.EMPTY_STATE.addConstraint(relation, BooleanConstraint.TRUE);
    Set<LearnedConstraint> learnedConstraints = child.learnedConstraints(parent);
    assertThat(learnedConstraints).hasSize(3);
    Constraint relationConstraint = learnedConstraints.stream().filter(lc -> lc.symbolicValue() == relation).findFirst().get().constraint();
    assertThat(relationConstraint).isEqualTo(BooleanConstraint.TRUE);
    Constraint sv1Constraint = learnedConstraints.stream().filter(lc -> lc.symbolicValue() == sv1).findFirst().get().constraint();
    assertThat(sv1Constraint).isNull();
    Constraint sv2Constraint = learnedConstraints.stream().filter(lc -> lc.symbolicValue() == sv2).findFirst().get().constraint();
    assertThat(sv2Constraint).isNull();
  }

  @Test
  public void test_learned_associations() throws Exception {
    ProgramState parent = ProgramState.EMPTY_STATE;
    ProgramState child = ProgramState.EMPTY_STATE;
    assertThat(child.learnedAssociations(parent)).isEmpty();

    Symbol symbol = new JavaSymbol.VariableJavaSymbol(0, "symbol", mock(JavaSymbol.MethodJavaSymbol.class));
    SymbolicValue sv1 = new SymbolicValue(1);
    child = child.put(symbol, sv1);
    Set<LearnedAssociation> learnedAssociations = child.learnedAssociations(parent);
    assertThat(learnedAssociations).hasSize(1);
    LearnedAssociation learnedAssociation = learnedAssociations.iterator().next();
    assertThat(learnedAssociation.symbolicValue()).isEqualTo(sv1);
    assertThat(learnedAssociation.symbol()).isEqualTo(symbol);

    assertThat(child.learnedAssociations(child)).isEmpty();
  }


  @Test
  public void test_peek_nth_value() {
    ProgramState state = ProgramState.EMPTY_STATE;
    ProgramState finalState = state;
    assertThatThrownBy(() -> finalState.peekValue(0)).isInstanceOf(IllegalStateException.class);
    SymbolicValue sv1 = new SymbolicValue(1);
    state = state.stackValue(sv1);
    assertThat(state.peekValue(0)).isEqualTo(sv1);
    SymbolicValue sv2 = new SymbolicValue(2);
    state = state.stackValue(sv2);
    assertThat(state.peekValue(1)).isEqualTo(sv1);
  }
}
