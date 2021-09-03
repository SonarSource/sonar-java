/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.se.ProgramState.Pop;
import org.sonar.java.se.checks.UnclosedResourcesCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValueTestUtil;
import org.sonar.plugins.java.api.semantic.Symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.se.utils.SETestUtils.variable;

class ProgramStateTest {

  @Test
  void testing_equals() {
    SymbolicValue sv1 = new SymbolicValue();
    ProgramState state = ProgramState.EMPTY_STATE.addConstraint(sv1, ObjectConstraint.NOT_NULL);
    ProgramState state2 = ProgramState.EMPTY_STATE.addConstraint(sv1, ObjectConstraint.NOT_NULL);
    ProgramState state3 = ProgramState.EMPTY_STATE.addConstraint(sv1, ObjectConstraint.NULL);
    assertThat(state)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isNotEqualTo(state3)
      .isEqualTo(state2);
  }

  @Test
  void testStackUnstack() {
    SymbolicValue sv1 = new SymbolicValue();
    ProgramState state = ProgramState.EMPTY_STATE.stackValue(sv1);
    assertThat(state.peekValue()).isSameAs(sv1);
    SymbolicValue sv2 = new SymbolicValue();
    state = state.stackValue(sv2);
    List<SymbolicValue> values = state.peekValues(2);
    assertThat(values).hasSize(2).containsSequence(sv2, sv1);
    try {
      state.peekValues(3);
      Assertions.fail("Able to retrieve more values than there are actually on the stack!");
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
  void testToString() {
    SymbolicValue sv3 = new SymbolicValue() {
      @Override
      public String toString() {
        return "SV_3";
      }
    };
    ProgramState state = ProgramState.EMPTY_STATE.stackValue(sv3);
    Symbol variable = variable("x");
    SymbolicValue sv4 = new SymbolicValue() {
      @Override
      public String toString() {
        return "SV_4";
      }
    };
    state = state.put(variable, sv4);
    SymbolicValue sv5 = new SymbolicValue() {
      @Override
      public String toString() {
        return "SV_5";
      }
    };
    state = state.stackValue(sv5, variable);
    // FIXME to string is not really nice by displaying classes and order is not guaranteed.
    assertThat(state.toString()).contains("A#x->SV_4", "SV_NULL", "SV_TRUE", "SV_FALSE", "A#x->SV_5", "SV_3");
    // .isEqualTo("{ A#x->SV_4} { SV_0_NULL-> class org.sonar.java.se.constraint.ObjectConstraint->NULL SV_1_TRUE-> class
    // org.sonar.java.se.constraint.BooleanConstraint->TRUE class org.sonar.java.se.constraint.ObjectConstraint->NOT_NULL SV_2_FALSE-> class
    // org.sonar.java.se.constraint.BooleanConstraint->FALSE class org.sonar.java.se.constraint.ObjectConstraint->NOT_NULL} { [SV_5, SV_3] }
    // { A#x } ");
  }

  @Test
  void testAddingSameConstraintTwice() {
    ProgramState state = ProgramState.EMPTY_STATE;
    SymbolicValue sv3 = new SymbolicValue();
    assertThat(state.getConstraint(sv3, ObjectConstraint.class)).isNull();
    state = state.addConstraint(sv3, ObjectConstraint.NOT_NULL);
    assertThat(state.getConstraint(sv3, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    ProgramState next = state.addConstraint(sv3, ObjectConstraint.NOT_NULL);
    assertThat(next).isSameAs(state);
  }

  @Test
  void test_learned_constraint() {
    ProgramState parent = ProgramState.EMPTY_STATE;
    ProgramState child = ProgramState.EMPTY_STATE;
    assertThat(child.learnedConstraints(parent)).isEmpty();
    SymbolicValue sv = new SymbolicValue();
    child = child.addConstraint(sv, ObjectConstraint.NULL);
    Set<LearnedConstraint> learnedConstraints = child.learnedConstraints(parent);
    assertThat(learnedConstraints).hasSize(1);
    LearnedConstraint lc = learnedConstraints.iterator().next();
    assertThat(lc.symbolicValue()).isEqualTo(sv);
    assertThat(lc.constraint()).isEqualTo(ObjectConstraint.NULL);
  }

  @Test
  void test_learned_constraint_binary_SV() {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    RelationalSymbolicValue relation = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.EQUAL);
    SymbolicValueTestUtil.computedFrom(relation, sv1, sv2);
    ProgramState parent = ProgramState.EMPTY_STATE;
    ProgramState child = ProgramState.EMPTY_STATE.addConstraint(relation, BooleanConstraint.TRUE);
    Set<LearnedConstraint> learnedConstraints = child.learnedConstraints(parent);
    assertThat(learnedConstraints).hasSize(1);
    Constraint relationConstraint = SetUtils.getOnlyElement(learnedConstraints).constraint();
    assertThat(relationConstraint).isEqualTo(BooleanConstraint.TRUE);
  }

  @Test
  void test_learned_associations() throws Exception {
    ProgramState parent = ProgramState.EMPTY_STATE;
    ProgramState child = ProgramState.EMPTY_STATE;
    assertThat(child.learnedAssociations(parent)).isEmpty();

    Symbol symbol = variable("symbol");
    SymbolicValue sv1 = new SymbolicValue();
    child = child.put(symbol, sv1);
    Set<LearnedAssociation> learnedAssociations = child.learnedAssociations(parent);
    assertThat(learnedAssociations).hasSize(1);
    LearnedAssociation learnedAssociation = learnedAssociations.iterator().next();
    assertThat(learnedAssociation.symbolicValue()).isEqualTo(sv1);
    assertThat(learnedAssociation.symbol()).isEqualTo(symbol);

    assertThat(child.learnedAssociations(child)).isEmpty();
  }

  @Test
  void test_peek_nth_value() {
    ProgramState state = ProgramState.EMPTY_STATE;
    ProgramState finalState = state;
    assertThatThrownBy(() -> finalState.peekValue(0)).isInstanceOf(IllegalStateException.class);
    SymbolicValue sv1 = new SymbolicValue();
    state = state.stackValue(sv1);
    assertThat(state.peekValue(0)).isEqualTo(sv1);
    SymbolicValue sv2 = new SymbolicValue();
    state = state.stackValue(sv2);
    assertThat(state.peekValue(1)).isEqualTo(sv1);
  }

  @Test
  void test_setting_constraint_on_relational_sv() throws Exception {
    RelationalSymbolicValue rel = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.EQUAL);
    SymbolicValueTestUtil.computedFrom(rel, new SymbolicValue(), new SymbolicValue());
    assertThatThrownBy(() -> ProgramState.EMPTY_STATE.addConstraint(rel, BooleanConstraint.FALSE))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Relations stored in PS should always use TRUE constraint");
  }

  @Test
  void assignment_order_should_not_lead_to_different_state() throws Exception {
    SymbolicValue sv = new SymbolicValue();
    Symbol var1 = variable("var1");
    Symbol var2 = variable("var2");
    ProgramState ps1 = ProgramState.EMPTY_STATE;
    ps1 = ps1.put(var1, sv);
    ps1 = ps1.put(var2, sv);
    ProgramState ps2 = ProgramState.EMPTY_STATE;
    ps2 = ps2.put(var2, sv);
    ps2 = ps2.put(var1, sv);
    assertThat(ps1)
      .isEqualTo(ps2)
      .hasSameHashCodeAs(ps2);
  }

  @Test
  void test_symbols_on_stack() {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv = new SymbolicValue();
    Symbol.VariableSymbol symbol = variable("a");
    ps = ps.stackValue(sv, symbol);
    Pop pop = ps.unstackValue(1);
    assertThat(ps.peekValue()).isEqualTo(sv);
    assertThat(ps.peekValueSymbol().symbol()).isEqualTo(symbol);
    assertThat(pop.valuesAndSymbols.get(0).symbol()).isEqualTo(symbol);
    assertThat(pop.valuesAndSymbols.get(0).symbolicValue()).isEqualTo(sv);
  }

  @Test
  void test_symbol_should_not_change_equals() throws Exception {
    ProgramState ps1 = ProgramState.EMPTY_STATE;
    ProgramState ps2 = ProgramState.EMPTY_STATE;
    SymbolicValue sv = new SymbolicValue();
    Symbol.VariableSymbol symbol = variable("a");
    ps1 = ps1.stackValue(sv);
    ps2 = ps2.stackValue(sv, symbol);
    assertThat(ps1).isEqualTo(ps2);
    assertThat(SetUtils.immutableSetOf(ps1, ps2)).hasSize(1);
  }

  @Test
  void test_adding_constraint_transitively() throws Exception {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    RelationalSymbolicValue relSV = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.EQUAL);
    SymbolicValueTestUtil.computedFrom(relSV, sv1, sv2);
    ps = ps.addConstraint(relSV, BooleanConstraint.TRUE);
    UnclosedResourcesCheck.ResourceConstraint constraint = UnclosedResourcesCheck.ResourceConstraint.OPEN;
    ps = ps.addConstraintTransitively(sv1, constraint);
    assertThat(ps.getConstraint(sv2, constraint.getClass())).isEqualTo(constraint);
  }

}
