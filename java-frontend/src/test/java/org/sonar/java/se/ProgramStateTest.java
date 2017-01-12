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

import junit.framework.Assert;
import org.junit.Test;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.Symbols;
import org.sonar.java.se.ProgramState.Pop;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgramStateTest {

  @Test
  public void testing_equals() {
    SymbolicValue sv1 = new SymbolicValue(1);
    ProgramState state = ProgramState.EMPTY_STATE.addConstraint(sv1, ObjectConstraint.notNull());
    assertThat(state.equals(null)).isFalse();
    assertThat(state.equals(new String())).isFalse();
    ProgramState state2 = ProgramState.EMPTY_STATE.addConstraint(sv1, ObjectConstraint.notNull());
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
    assertThat(state.toString()).isEqualTo("{ A#x->SV_4}  { SV_0_NULL->NULL SV_1_TRUE->TRUE SV_2_FALSE->FALSE} { [SV_5, SV_3] } { A#x } ");
  }

  @Test
  public void testAddingSameConstraintTwice() {
    ProgramState state = ProgramState.EMPTY_STATE;
    SymbolicValue sv3 = new SymbolicValue(3);
    assertThat(state.getConstraint(sv3)).isNull();
    state = state.addConstraint(sv3, ObjectConstraint.notNull());
    assertThat(state.getConstraint(sv3)).isEqualTo(ObjectConstraint.notNull());
    ProgramState next = state.addConstraint(sv3, ObjectConstraint.notNull());
    assertThat(next).isSameAs(state);
  }
}
