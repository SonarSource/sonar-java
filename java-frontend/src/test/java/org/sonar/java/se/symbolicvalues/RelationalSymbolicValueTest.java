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
package org.sonar.java.se.symbolicvalues;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Test;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RelationalSymbolicValueTest {

  ConstraintManager constraintManager = new ConstraintManager();
  SymbolicValue a = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_1";
    }
  };
  SymbolicValue b = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_2";
    }
  };
  SymbolicValue c = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_3";
    }
  };

  @Test
  public void test_create() throws Exception {
    ImmutableList<SymbolicValue> computedFrom = ImmutableList.of(b, a);
    assertThat(create(Tree.Kind.EQUAL_TO, computedFrom)).hasToString("SV_1==SV_2");
    assertThat(create(Tree.Kind.NOT_EQUAL_TO, computedFrom)).hasToString("SV_1!=SV_2");
    assertThat(create(Tree.Kind.GREATER_THAN, computedFrom)).hasToString("SV_2<SV_1");
    assertThat(create(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, computedFrom)).hasToString("SV_1>=SV_2");
    assertThat(create(Tree.Kind.LESS_THAN, computedFrom)).hasToString("SV_1<SV_2");
    assertThat(create(Tree.Kind.LESS_THAN_OR_EQUAL_TO, computedFrom)).hasToString("SV_2>=SV_1");
  }

  private SymbolicValue create(Tree.Kind kind, ImmutableList<SymbolicValue> computedFrom) {
    return constraintManager
      .createBinarySymbolicValue(new BinaryExpressionTreeImpl(kind, mock(ExpressionTree.class), mock(InternalSyntaxToken.class), mock(ExpressionTree.class)), computedFrom);
  }


  @Test
  public void test_transitive_constraint_copy() throws Exception {
    SymbolicValue aNEb = create(Tree.Kind.NOT_EQUAL_TO, ImmutableList.of(b, a));
    SymbolicValue bNEc = create(Tree.Kind.NOT_EQUAL_TO, ImmutableList.of(c, b));
    ProgramState programState = ProgramState.EMPTY_STATE;
    List<ProgramState> programStates = aNEb.setConstraint(programState, BooleanConstraint.TRUE);
    programState = Iterables.getOnlyElement(programStates);
    programStates = bNEc.setConstraint(programState, BooleanConstraint.TRUE);
    programState = Iterables.getOnlyElement(programStates);

    SymbolicValue aNEc = create(Tree.Kind.NOT_EQUAL_TO, ImmutableList.of(c, a));
    programStates = aNEc.setConstraint(programState, BooleanConstraint.FALSE);
    assertThat(programStates).hasSize(1);
    programStates = aNEc.setConstraint(programState, BooleanConstraint.TRUE);
    assertThat(programStates).hasSize(1);
  }

  @Test
  public void test_equals_hashCode() throws Exception {
    SymbolicValue ab = create(Tree.Kind.EQUAL_TO, ImmutableList.of(a, b));
    SymbolicValue ba = create(Tree.Kind.EQUAL_TO, ImmutableList.of(b, a));
    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());

    ab = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS);
    ab.computedFrom(ImmutableList.of(a, b));
    ba = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS);
    ba.computedFrom(ImmutableList.of(b, a));
    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());

    ab = create(Tree.Kind.LESS_THAN, ImmutableList.of(a, b));
    ba = create(Tree.Kind.LESS_THAN, ImmutableList.of(b, a));
    assertThat(ab).isNotEqualTo(ba);
  }

  @Test
  public void test_constraint_copy() throws Exception {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue a = new SymbolicValue();
    SymbolicValue b = new SymbolicValue();
    List<ProgramState> newProgramStates = a.setConstraint(ps, DivisionByZeroCheck.ZeroConstraint.ZERO);
    ps = Iterables.getOnlyElement(newProgramStates);
    // 0 >= b
    SymbolicValue aGEb = create(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, ImmutableList.of(b, a));
    newProgramStates = aGEb.setConstraint(ps, BooleanConstraint.TRUE);
    ps = Iterables.getOnlyElement(newProgramStates);

    // Zero constraint should stay when Zero is >= to SV without any constraint
    assertThat(ps.getConstraint(a, DivisionByZeroCheck.ZeroConstraint.class)).isEqualTo(DivisionByZeroCheck.ZeroConstraint.ZERO);
    assertThat(ps.getConstraint(b, DivisionByZeroCheck.ZeroConstraint.class)).isNull();
  }
}
