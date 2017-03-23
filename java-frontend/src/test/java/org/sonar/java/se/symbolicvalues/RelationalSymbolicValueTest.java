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

import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

  private final List<Tree.Kind> operators = Arrays.asList(
    Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO,
    Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
    Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO);

  @Test
  public void test_create() throws Exception {
    assertThat(create(Tree.Kind.EQUAL_TO, b, a)).hasToString("SV_1==SV_2");
    assertThat(create(Tree.Kind.NOT_EQUAL_TO, b, a)).hasToString("SV_1!=SV_2");
    assertThat(create(Tree.Kind.GREATER_THAN, b, a)).hasToString("SV_2<SV_1");
    assertThat(create(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, b, a)).hasToString("SV_1>=SV_2");
    assertThat(create(Tree.Kind.LESS_THAN, b, a)).hasToString("SV_1<SV_2");
    assertThat(create(Tree.Kind.LESS_THAN_OR_EQUAL_TO, b, a)).hasToString("SV_2>=SV_1");
  }

  private RelationalSymbolicValue create(Tree.Kind kind, SymbolicValue... computedFrom) {
    return (RelationalSymbolicValue) constraintManager
      .createBinarySymbolicValue(new BinaryExpressionTreeImpl(kind, mock(ExpressionTree.class), mock(InternalSyntaxToken.class), mock(ExpressionTree.class)),
        Arrays.asList(computedFrom));
  }


  @Test
  public void test_transitive_constraint_copy() throws Exception {
    SymbolicValue aNEb = create(Tree.Kind.NOT_EQUAL_TO, b, a);
    SymbolicValue bNEc = create(Tree.Kind.NOT_EQUAL_TO, c, b);
    ProgramState programState = ProgramState.EMPTY_STATE;
    List<ProgramState> programStates = aNEb.setConstraint(programState, BooleanConstraint.TRUE);
    programState = Iterables.getOnlyElement(programStates);
    programStates = bNEc.setConstraint(programState, BooleanConstraint.TRUE);
    programState = Iterables.getOnlyElement(programStates);

    SymbolicValue aNEc = create(Tree.Kind.NOT_EQUAL_TO, c, a);
    programStates = aNEc.setConstraint(programState, BooleanConstraint.FALSE);
    assertThat(programStates).hasSize(1);
    programStates = aNEc.setConstraint(programState, BooleanConstraint.TRUE);
    assertThat(programStates).hasSize(1);
  }

  @Test
  public void test_equals_hashCode() throws Exception {
    SymbolicValue ab = create(Tree.Kind.EQUAL_TO, a, b);
    SymbolicValue ba = create(Tree.Kind.EQUAL_TO, b, a);
    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());

    ab = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, a, b);
    ba = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, b, a);
    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());

    ab = create(Tree.Kind.LESS_THAN, a, b);
    ba = create(Tree.Kind.LESS_THAN, b, a);
    assertThat(ab).isNotEqualTo(ba);

    SymbolicValue eq = create(Tree.Kind.EQUAL_TO, a, b);
    SymbolicValue eq1 = create(Tree.Kind.EQUAL_TO, b, b);
    SymbolicValue eq2 = create(Tree.Kind.EQUAL_TO, a, a);
    SymbolicValue neq = create(Tree.Kind.NOT_EQUAL_TO, b, a);
    assertThat(eq).isNotEqualTo(neq);
    assertThat(eq).isEqualTo(eq);
    assertThat(eq).isNotEqualTo(eq1);
    assertThat(eq).isNotEqualTo(eq2);
    assertThat(eq).isNotEqualTo(null);
    assertThat(eq).isNotEqualTo(new Object());


    SymbolicValue ab1 = create(Tree.Kind.LESS_THAN, a, b);
    SymbolicValue ab2 = create(Tree.Kind.LESS_THAN, a, b);
    SymbolicValue ab3 = create(Tree.Kind.LESS_THAN, a, new SymbolicValue());
    SymbolicValue ab4 = create(Tree.Kind.LESS_THAN, new SymbolicValue(), b);
    assertThat(ab1).isEqualTo(ab2);
    assertThat(ab1).isNotEqualTo(ab3);
    assertThat(ab1).isNotEqualTo(ab4);
  }

  @Test
  public void test_constraint_copy() throws Exception {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue a = new SymbolicValue();
    SymbolicValue b = new SymbolicValue();
    List<ProgramState> newProgramStates = a.setConstraint(ps, DivisionByZeroCheck.ZeroConstraint.ZERO);
    ps = Iterables.getOnlyElement(newProgramStates);
    // 0 >= b
    SymbolicValue aGEb = create(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, b, a);
    newProgramStates = aGEb.setConstraint(ps, BooleanConstraint.TRUE);
    ps = Iterables.getOnlyElement(newProgramStates);

    // Zero constraint should stay when Zero is >= to SV without any constraint
    assertThat(ps.getConstraint(a, DivisionByZeroCheck.ZeroConstraint.class)).isEqualTo(DivisionByZeroCheck.ZeroConstraint.ZERO);
    assertThat(ps.getConstraint(b, DivisionByZeroCheck.ZeroConstraint.class)).isNull();
  }

  @Test
  public void test_setting_operands() throws Exception {
    RelationalSymbolicValue relSV = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.EQUAL, a, b);
    assertThatThrownBy(() -> relSV.computedFrom(Arrays.asList(b, a)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Operands already set!");

    assertThatThrownBy(() -> relSV.computedFrom(Arrays.asList(a, b, a)))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void test_transitive_deduction() throws Exception {
    List<String> actual = new ArrayList<>();
    RelationalSymbolicValue eq = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, b, c);
    RelationalSymbolicValue neq = eq.inverse();
    for (Tree.Kind r : operators) {
      RelationalSymbolicValue first = create(r, b, a);
      for (Tree.Kind t : operators) {
        RelationalSymbolicValue second = create(t, c, b);
        RelationalSymbolicValue deduced = first.deduceTransitiveOrSimplified(second);
        actual.add(String.format("%s && %s => %s", relationToString(r, a, b), relationToString(t, b, c), nullableToCollection(deduced)));
      }
      actual.add(String.format("%s && %s.EQ.%s => %s", relationToString(r, a, b), b, c, nullableToCollection(first.deduceTransitiveOrSimplified(eq))));
      actual.add(String.format("%s && %s.NE.%s => %s", relationToString(r, a, b), b, c, nullableToCollection(first.deduceTransitiveOrSimplified(neq))));
    }
    RelationalSymbolicValue eqAB = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, a, b);
    actual.addAll(methodEquals(eq, neq, eqAB));
    RelationalSymbolicValue neqAB = eqAB.inverse();
    actual.addAll(methodEquals(eq, neq, neqAB));

    List<String> expected = IOUtils.readLines(getClass().getResourceAsStream("/relations/transitive.txt"));
    assertThat(actual).isEqualTo(expected);
  }

  private List<String> methodEquals(RelationalSymbolicValue eq, RelationalSymbolicValue neq, RelationalSymbolicValue eqAB) {
    List<String> actual = new ArrayList<>();
    for (Tree.Kind r : operators) {
      actual.add(String.format("%s && %s => %s", eqAB, relationToString(r, b, c), nullableToCollection(eqAB.deduceTransitiveOrSimplified(create(r, c, b)))));
    }
    actual.add(String.format("%s && %s.EQ.%s => %s", eqAB, b, c, nullableToCollection(eqAB.deduceTransitiveOrSimplified(eq))));
    actual.add(String.format("%s && %s.NE.%s => %s", eqAB, b, c, nullableToCollection(eqAB.deduceTransitiveOrSimplified(neq))));
    return actual;
  }

  private Collection<SymbolicValue> nullableToCollection(@Nullable RelationalSymbolicValue deduced) {
    return deduced == null ? Collections.emptySet() : Collections.singleton(deduced);
  }

  private String relationToString(Tree.Kind kind, SymbolicValue leftOp, SymbolicValue rightOp) {
    return leftOp.toString() + operatorToString(kind) + rightOp.toString();
  }

  private String operatorToString(Tree.Kind kind) {
    switch (kind) {
      case EQUAL_TO:
        return "==";
      case NOT_EQUAL_TO:
        return "!=";
      case GREATER_THAN:
        return ">";
      case GREATER_THAN_OR_EQUAL_TO:
        return ">=";
      case LESS_THAN:
        return "<";
      case LESS_THAN_OR_EQUAL_TO:
        return "<=";
      default:
        throw new IllegalStateException();
    }
  }
}
