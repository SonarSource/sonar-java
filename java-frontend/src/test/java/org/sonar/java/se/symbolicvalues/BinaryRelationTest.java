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

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.symbolicvalues.BinaryRelation.binaryRelation;
import static org.sonar.java.se.symbolicvalues.RelationState.FULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationState.UNFULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.GREATER_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN_OR_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.METHOD_EQUALS;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_METHOD_EQUALS;

public class BinaryRelationTest {

  private final SymbolicValue a = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_1";
    }
  };

  private final SymbolicValue b = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_2";
    }
  };

  private final SymbolicValue c = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_3";
    }
  };

  @Test
  public void test_normalization() {
    assertThat(binaryRelation(EQUAL, a, b)).hasToString("SV_1==SV_2");
    assertThat(binaryRelation(NOT_EQUAL, a, b)).hasToString("SV_1!=SV_2");
    assertThat(binaryRelation(GREATER_THAN, a, b)).hasToString("SV_2<SV_1");
    assertThat(binaryRelation(GREATER_THAN_OR_EQUAL, a, b)).hasToString("SV_1>=SV_2");
    assertThat(binaryRelation(LESS_THAN, a, b)).hasToString("SV_1<SV_2");
    assertThat(binaryRelation(LESS_THAN_OR_EQUAL, a, b)).hasToString("SV_2>=SV_1");
    assertThat(binaryRelation(METHOD_EQUALS, a, b)).hasToString("SV_1.EQ.SV_2");
    assertThat(binaryRelation(NOT_METHOD_EQUALS, a, b)).hasToString("SV_1.NE.SV_2");
  }

  @Test
  public void test_same_operand() {
    assertThat(sameOperandResolution(EQUAL)).isEqualTo(FULFILLED);
    assertThat(sameOperandResolution(METHOD_EQUALS)).isEqualTo(FULFILLED);
    assertThat(sameOperandResolution(LESS_THAN_OR_EQUAL)).isEqualTo(FULFILLED);
    assertThat(sameOperandResolution(GREATER_THAN_OR_EQUAL)).isEqualTo(FULFILLED);

    assertThat(sameOperandResolution(NOT_EQUAL)).isEqualTo(UNFULFILLED);
    assertThat(sameOperandResolution(NOT_METHOD_EQUALS)).isEqualTo(UNFULFILLED);
    assertThat(sameOperandResolution(LESS_THAN)).isEqualTo(UNFULFILLED);
    assertThat(sameOperandResolution(GREATER_THAN)).isEqualTo(UNFULFILLED);
  }

  private RelationState sameOperandResolution(Kind kind) {
    return binaryRelation(kind, a, a).resolveState(emptyList());
  }

  @Test
  public void test_transitive_relationship() throws Exception {
    BinaryRelation ab = relSV(EQUAL, a, b).binaryRelation();
    BinaryRelation ba = relSV(EQUAL, b, a).binaryRelation();
    BinaryRelation bc = relSV(LESS_THAN, b, c).binaryRelation();
    assertThat(ab.potentiallyTransitiveWith(ab)).isFalse();
    assertThat(ab.potentiallyTransitiveWith(ba)).isFalse();
    assertThat(ba.potentiallyTransitiveWith(ab)).isFalse();
    assertThat(bc.potentiallyTransitiveWith(bc)).isFalse();

    assertThat(ab.potentiallyTransitiveWith(bc)).isTrue();
    assertThat(ba.potentiallyTransitiveWith(bc)).isTrue();
  }

  @Test
  public void test_common_different_operand() throws Exception {
    BinaryRelation ab = relSV(EQUAL, a, b).binaryRelation();
    BinaryRelation bc = relSV(EQUAL, b, c).binaryRelation();
    assertThat(ab.commonOperand(bc)).isEqualTo(b);
    assertThat(bc.commonOperand(ab)).isEqualTo(b);
    assertThat(ab.differentOperand(bc)).isEqualTo(a);
    assertThat(bc.differentOperand(ab)).isEqualTo(c);
  }

  @Test
  public void test_direct_deduction() throws Exception {
    List<String> actual = new ArrayList<>();
    for (Kind given : Kind.values()) {
      actual.addAll(resolveRelationStateForAllKinds(relSV(given, a, b)));
      actual.addAll(resolveRelationStateForAllKinds(relSV(given, b, a)));
    }
    List<String> expected = IOUtils.readLines(getClass().getResourceAsStream("/relations/direct.txt"));
    assertThat(actual).isEqualTo(expected);
  }

  private List<String> resolveRelationStateForAllKinds(RelationalSymbolicValue known) {
    List<String> actual = new ArrayList<>();
    for (Kind when : Kind.values()) {
      RelationalSymbolicValue test = relSV(when, a, b);
      RelationState relationState = test.binaryRelation().resolveState(given(known));
      actual.add(String.format("given %s when %s -> %s", relationToString(known), relationToString(test), relationState));
    }
    return actual;
  }

  @Test
  public void test_transitive_deduction() throws Exception {
    List<String> actual = new ArrayList<>();
    for (Kind r : Kind.values()) {
      for (Kind t : Kind.values()) {
        RelationalSymbolicValue first = relSV(r, a, b);
        RelationalSymbolicValue second = relSV(t, b, c);
        Collection<BinaryRelation> deduced = deduce(first, second);
        actual.add(String.format("%s && %s => %s", relationToString(first), relationToString(second), deduced));
      }
    }
    List<String> expected = IOUtils.readLines(getClass().getResourceAsStream("/relations/transitive.txt"));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void test_conjuction_equal() throws Exception {
    RelationalSymbolicValue aLEb = relSV(LESS_THAN_OR_EQUAL, a, b);
    RelationalSymbolicValue bLEa = relSV(LESS_THAN_OR_EQUAL, b, a);
    RelationalSymbolicValue aEb = relSV(EQUAL, a, b);
    RelationState state = aEb.binaryRelation().resolveState(given(aLEb, bLEa));
    assertThat(state).isEqualTo(FULFILLED);
  }

  @Test
  public void test_transitive_GE() throws Exception {
    RelationalSymbolicValue ab = relSV(GREATER_THAN_OR_EQUAL, a, b);
    RelationalSymbolicValue bc = relSV(GREATER_THAN_OR_EQUAL, b, c);
    Collection<BinaryRelation> deduced = deduce(ab, bc);
    assertThat(deduced).containsExactly(relSV(GREATER_THAN_OR_EQUAL, a, c).binaryRelation());
  }

  private Collection<BinaryRelation> deduce(RelationalSymbolicValue rel1, RelationalSymbolicValue rel2) {
    BinaryRelation binaryRelation = rel1.binaryRelation().deduceTransitiveOrSimplified(rel2.binaryRelation());
    return binaryRelation == null ? Collections.emptySet() : Collections.singleton(binaryRelation);
  }

  @Test
  public void test_transitive_method_equals() throws Exception {
    RelationalSymbolicValue equalAB = relSV(EQUAL, a, b);
    RelationalSymbolicValue methodEqualBC = relSV(METHOD_EQUALS, b, c);
    Collection<BinaryRelation> deduce = deduce(equalAB, methodEqualBC);
    BinaryRelation expected = relSV(METHOD_EQUALS, a, c).binaryRelation();
    assertThat(deduce).containsExactly(expected);
    deduce = deduce(methodEqualBC, equalAB);
    assertThat(deduce).containsExactly(expected);
  }

  @Test
  public void test_chained_transitivity() throws Exception {
    int chainLength = 21;
    SymbolicValue[] sv = new SymbolicValue[chainLength];
    List<BinaryRelation> given = new ArrayList<>();
    sv[0] = new SymbolicValue();
    for (int i = 1; i < chainLength; i++) {
      sv[i] = new SymbolicValue();
      given.add(relSV(LESS_THAN, sv[i - 1], sv[i]).binaryRelation());
    }
    BinaryRelation firstLessThanLast = relSV(LESS_THAN, sv[0], sv[chainLength - 1]).binaryRelation();
    RelationState relationState = firstLessThanLast.resolveState(given);
    assertThat(relationState).isEqualTo(FULFILLED);
  }

  @Test
  public void test_not_equals_is_not_transitive() throws Exception {
    BinaryRelation aNEb = binaryRelation(NOT_EQUAL, a, b);
    BinaryRelation bNEc = binaryRelation(NOT_EQUAL, b, c);
    BinaryRelation relation = aNEb.deduceTransitiveOrSimplified(bNEc);
    assertThat(relation).isNull();
  }

  private RelationalSymbolicValue relSV(Kind kind, SymbolicValue leftOp, SymbolicValue rightOp) {
    RelationalSymbolicValue relationalSymbolicValue = new RelationalSymbolicValue(kind);
    relationalSymbolicValue.computedFrom(Arrays.asList(rightOp, leftOp));
    return relationalSymbolicValue;
  }

  private List<BinaryRelation> given(RelationalSymbolicValue... sv) {
    return Arrays.stream(sv).map(RelationalSymbolicValue::binaryRelation).collect(Collectors.toList());
  }

  private String relationToString(RelationalSymbolicValue rsv) {
    return String.valueOf(rsv.leftOp) + rsv.kind.operand + rsv.rightOp;
  }
}
