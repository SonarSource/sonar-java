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
import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.java.se.symbolicvalues.RelationState.FULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationState.UNDETERMINED;
import static org.sonar.java.se.symbolicvalues.RelationState.UNFULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.GREATER_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN_OR_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.METHOD_EQUALS;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_METHOD_EQUALS;

public class BinaryRelationsTest {

  private static int NUMBER_OF_VALUES = 5;

  private static SymbolicValue[] values = new SymbolicValue[NUMBER_OF_VALUES];
  private static SymbolicValue SVa;
  private static SymbolicValue SVb;
  private static SymbolicValue SVc;

  @BeforeClass
  public static void initialize() {
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      values[i] = new SymbolicValue(i);
    }
    SVa = values[0];
    SVb = values[1];
    SVc = values[2];
  }

  @Test
  public void testEqual() {
    verifyEqualResolution(relationsWithOneValue(EQUAL));
  }

  private List<BinaryRelation> relationsWithOneValue(Kind kind) {
    List<BinaryRelation> constraints = new ArrayList<>();
    for (int i = 1; i < values.length; i++) {
      constraints.add(relation(kind, SVa, values[i]));
    }
    return constraints;
  }

  @Test
  public void testEqualChained() {
    verifyEqualResolution(relationsBetweenValue(EQUAL));
  }

  private List<BinaryRelation> relationsBetweenValue(Kind kind) {
    List<BinaryRelation> constraints = new ArrayList<>();
    for (int i = 1; i < values.length; i++) {
      constraints.add(relation(kind, values[i-1], values[i]));
    }
    return constraints;
  }

  private void verifyEqualResolution(List<BinaryRelation> constraints) {
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          BinaryRelation equalRelation = relation(EQUAL, values[i], values[j]);
          assertThat(equalRelation.resolveState(constraints)).as(equalRelation.toString()).isEqualTo(FULFILLED);
          BinaryRelation notEqualRelation = relation(NOT_EQUAL, values[i], values[j]);
          assertThat(notEqualRelation.resolveState(constraints)).as(notEqualRelation.toString()).isEqualTo(UNFULFILLED);
        }
      }
    }
  }

  @Test
  public void testNotEqual() {
    List<BinaryRelation> constraints = relationsWithOneValue(NOT_EQUAL);
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          BinaryRelation notEqualRelation = relation(NOT_EQUAL, values[i], values[j]);
          RelationState notEqualResult = notEqualRelation.resolveState(constraints);
          BinaryRelation equalRelation = relation(EQUAL, values[i], values[j]);
          RelationState equalResult = equalRelation.resolveState(constraints);
          if (i == 0 || j == 0) {
            assertThat(notEqualResult).as(notEqualRelation.toString()).isEqualTo(FULFILLED);
            assertThat(equalResult).as(equalRelation.toString()).isEqualTo(UNFULFILLED);
          } else {
            assertThat(notEqualResult).as(notEqualRelation.toString()).isEqualTo(UNDETERMINED);
            assertThat(equalResult).as(equalRelation.toString()).isEqualTo(UNDETERMINED);
          }
        }
      }
    }
  }

  @Test
  public void testNotEqualChained() {
    List<BinaryRelation> constraints = relationsBetweenValue(NOT_EQUAL);
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          BinaryRelation notEqualRelation = relation(NOT_EQUAL, values[i], values[j]);
          RelationState notEqualResult = notEqualRelation.resolveState(constraints);
          BinaryRelation equalRelation = relation(EQUAL, values[i], values[j]);
          RelationState equalResult = equalRelation.resolveState(constraints);
          if (Math.abs(j - i) == 1) {
            assertThat(notEqualResult).as(notEqualRelation.toString()).isEqualTo(FULFILLED);
            assertThat(equalResult).as(equalRelation.toString()).isEqualTo(UNFULFILLED);
          } else {
            assertThat(notEqualResult).as(notEqualRelation.toString()).isEqualTo(UNDETERMINED);
            assertThat(equalResult).as(equalRelation.toString()).isEqualTo(UNDETERMINED);
          }
        }
      }
    }
  }

  @Test
  public void a_equal_b_and_b_notEqual_c_implies_a_notEqual_c() {
    BinaryRelation r1 = relation(EQUAL, SVa, SVb);
    BinaryRelation r2 = relation(NOT_EQUAL, SVb, SVc);
    BinaryRelation r3 = relation(NOT_EQUAL, SVa, SVc);
    assertRelationHasState(r3, ImmutableList.of(r1, r2), FULFILLED);
  }

  @Test
  public void a_equal_b_and_b_equal_c_implies_not_a_notEqual_c() {
    checkImplies(EQUAL, EQUAL, NOT_EQUAL, UNFULFILLED);
  }

  @Test
  public void a_equal_b_and_a_equal_c_implies_b_equal_c() {
    checkImplies(EQUAL, EQUAL, EQUAL, FULFILLED);
  }

  private void checkImplies(Kind rel1, Kind rel2, Kind implied, RelationState expect) {
    BinaryRelation r1 = relation(rel1, SVa, SVb);
    BinaryRelation r2 = relation(rel2, SVa, SVc);
    assertRelationHasState(relation(implied, SVa, SVb), ImmutableList.of(r1, r2), expect);
  }

  @Test
  public void loopRelations() {
    List<BinaryRelation> knownRelations = ImmutableList.of(
      relation(NOT_EQUAL, SVa, SVb),
      relation(NOT_EQUAL, SVb, SVc),
      relation(NOT_EQUAL, SVc, values[3]));
    BinaryRelation checked = relation(NOT_EQUAL, SVa, values[3]);
    assertRelationHasState(checked, knownRelations, UNDETERMINED);
  }

  @Test
  public void reversedTransitive() {
    List<BinaryRelation> knownRelations = ImmutableList.of(
      relation(EQUAL, SVa, SVb), // a==b
      relation(EQUAL, SVa, SVc), // a==c
      relation(EQUAL, SVa, values[3]));// a==d
    BinaryRelation checked = relation(EQUAL, SVb, values[3]); // b==d?
    assertRelationHasState(checked, knownRelations, FULFILLED);
  }

  @Test
  public void transitiveA() throws Exception {
    List<BinaryRelation> knownRelations = ImmutableList.of(
      relation(EQUAL, SVa, SVb), // a==b
      relation(NOT_EQUAL, SVb, SVc));// b!=c
    BinaryRelation checked = relation(NOT_EQUAL, SVa, SVc); // a!=c?
    assertRelationHasState(checked, knownRelations, FULFILLED);
  }

  @Test
  public void transitiveB() {
    List<BinaryRelation> knownRelations = ImmutableList.of(
      relation(EQUAL, SVa, SVb), // a==b
      relation(NOT_EQUAL, SVc, SVb));// c!=b
    BinaryRelation checked = relation(NOT_EQUAL, SVa, SVc); // a!=c?
    assertRelationHasState(checked, knownRelations, FULFILLED);
  }

  @Test
  public void transitiveC() {
    List<BinaryRelation> knownRelations = ImmutableList.of(
      relation(EQUAL, SVa, SVb), // a==b
      relation(NOT_EQUAL, SVb, SVc));// b!=c
    BinaryRelation checked = relation(EQUAL, SVa, SVc); // a==c?
    assertRelationHasState(checked, knownRelations, UNFULFILLED);
  }

  @Test
  public void transitiveD() {
    List<BinaryRelation> knownRelations = ImmutableList.of(
      relation(EQUAL, SVa, SVb), // a==b
      relation(NOT_EQUAL, SVc, SVb));// c!=b
    BinaryRelation checked = relation(EQUAL, SVa, SVc); // a==c?
    assertRelationHasState(checked, knownRelations, UNFULFILLED);
  }

  private void assertRelationHasState(BinaryRelation checkedRelation, List<BinaryRelation> knownRelations, RelationState expectedState) {
    RelationState result = checkedRelation.resolveState(knownRelations);
    assertThat(result).as(checkedRelation.toString()).isEqualTo(expectedState);
  }

  @Test
  public void testInverse() {
    for (Kind kind : Kind.values()) {
      checkInverse(kind);
    }
  }

  private void checkInverse(Kind kind) {
    try {
      BinaryRelation relation = relation(kind, SVa, SVb);
      BinaryRelation inverseOfInverse = relation.inverse().inverse();
      assertThat(relation.kind).isEqualTo(inverseOfInverse.kind);
      assertThat(relation.leftOp).isEqualTo(inverseOfInverse.leftOp);
      assertThat(relation.rightOp).isEqualTo(inverseOfInverse.rightOp);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSymmetric() {
    for (Kind kind : Kind.values()) {
      checkSymmetric(kind);
    }
  }

  private void checkSymmetric(Kind kind) {
    try {
      BinaryRelation relation = relation(kind, SVa, SVb);
      BinaryRelation symmetricOfSymmetric = relation.symmetric().symmetric();
      assertThat(relation.kind).isEqualTo(symmetricOfSymmetric.kind);
      assertThat(relation.leftOp).isEqualTo(symmetricOfSymmetric.leftOp);
      assertThat(relation.rightOp).isEqualTo(symmetricOfSymmetric.rightOp);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void operandInRelationWithItself() throws Exception {
    RelationState[] expected = {FULFILLED, UNFULFILLED, UNFULFILLED, FULFILLED, UNFULFILLED, FULFILLED, FULFILLED, UNFULFILLED};
    for (Kind kind : Kind.values()) {
      BinaryRelation relation = relation(kind, SVa, SVa);
      assertRelationHasState(relation, Collections.<BinaryRelation>emptyList(), expected[kind.ordinal()]);
    }
  }

  private static BinaryRelation relation(Kind kind, SymbolicValue a, SymbolicValue b) {
    RelationalSymbolicValue value = new RelationalSymbolicValue(-1, kind);
    value.computedFrom(Lists.newArrayList(b, a));
    return value.binaryRelation();
  }

  private void checkTransitive(BinaryRelation hypothesis1, BinaryRelation hypothesis2, BinaryRelation checked, RelationState expected) {
    List<BinaryRelation> hypotheses = ImmutableList.of(hypothesis1, hypothesis2);
    RelationState actual = checked.resolveState(hypotheses);
    assertThat(actual).as(hypothesis1.toString() + " && " + hypothesis2.toString() + " => " + checked.toString()).isEqualTo(expected);
  }

  @Test
  public void testEqualImplies() {
    checkImplies(EQUAL, new RelationState[]{FULFILLED, UNFULFILLED, UNFULFILLED, FULFILLED, UNFULFILLED, FULFILLED, FULFILLED, UNFULFILLED});
  }

  @Test
  public void testNotEqualImplies() {
    checkImplies(NOT_EQUAL, new RelationState[]{UNFULFILLED, FULFILLED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNDETERMINED});
  }

  @Test
  public void testMethodEqualsImplies() {
    checkImplies(METHOD_EQUALS, new RelationState[]{UNDETERMINED, UNDETERMINED, UNFULFILLED, FULFILLED, UNFULFILLED, FULFILLED, FULFILLED, UNFULFILLED});
  }

  @Test
  public void testMethodNotEqualsImplies() {
    checkImplies(NOT_METHOD_EQUALS, new RelationState[]{UNFULFILLED, FULFILLED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNFULFILLED, FULFILLED});
  }

  @Test
  public void testGreaterThanImplies() {
    checkImplies(GREATER_THAN, EQUAL, UNFULFILLED, UNFULFILLED);
    checkImplies(GREATER_THAN, NOT_EQUAL, FULFILLED, FULFILLED);
    checkImplies(GREATER_THAN, GREATER_THAN, FULFILLED, UNFULFILLED);
    checkImplies(GREATER_THAN, GREATER_THAN_OR_EQUAL, FULFILLED, UNFULFILLED);
    checkImplies(GREATER_THAN, LESS_THAN, UNFULFILLED, FULFILLED);
    checkImplies(GREATER_THAN, LESS_THAN_OR_EQUAL, UNFULFILLED, FULFILLED);
    checkImplies(GREATER_THAN, METHOD_EQUALS, UNFULFILLED, UNFULFILLED);
    checkImplies(GREATER_THAN, NOT_METHOD_EQUALS, FULFILLED, FULFILLED);
  }

  @Test
  public void testGreaterThanOrEqualImplies() {
    checkImplies(GREATER_THAN_OR_EQUAL, EQUAL, UNDETERMINED);
    checkImplies(GREATER_THAN_OR_EQUAL, NOT_EQUAL, UNDETERMINED);
    checkImplies(GREATER_THAN_OR_EQUAL, GREATER_THAN, UNDETERMINED, UNFULFILLED);
    checkImplies(GREATER_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL, FULFILLED, UNDETERMINED);
    checkImplies(GREATER_THAN_OR_EQUAL, LESS_THAN, UNFULFILLED, UNDETERMINED);
    checkImplies(GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, UNDETERMINED, FULFILLED);
    checkImplies(GREATER_THAN_OR_EQUAL, METHOD_EQUALS, UNDETERMINED);
    checkImplies(GREATER_THAN_OR_EQUAL, NOT_METHOD_EQUALS, UNDETERMINED);
  }

  @Test
  public void testLessThanImplies() {
    checkImplies(LESS_THAN, EQUAL, UNFULFILLED);
    checkImplies(LESS_THAN, NOT_EQUAL, FULFILLED);
    checkImplies(LESS_THAN, GREATER_THAN, UNFULFILLED, FULFILLED);
    checkImplies(LESS_THAN, GREATER_THAN_OR_EQUAL, UNFULFILLED, FULFILLED);
    checkImplies(LESS_THAN, LESS_THAN, FULFILLED, UNFULFILLED);
    checkImplies(LESS_THAN, LESS_THAN_OR_EQUAL, FULFILLED, UNFULFILLED);
    checkImplies(LESS_THAN, METHOD_EQUALS, UNDETERMINED);
    checkImplies(LESS_THAN, NOT_METHOD_EQUALS, UNDETERMINED);
  }

  @Test
  public void testLessThanOrEqualImplies() {
    checkImplies(LESS_THAN_OR_EQUAL, EQUAL, UNDETERMINED);
    checkImplies(LESS_THAN_OR_EQUAL, NOT_EQUAL, UNDETERMINED);
    checkImplies(LESS_THAN_OR_EQUAL, GREATER_THAN, UNFULFILLED, UNDETERMINED);
    checkImplies(LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL, UNDETERMINED, FULFILLED);
    checkImplies(LESS_THAN_OR_EQUAL, LESS_THAN, UNDETERMINED, UNFULFILLED);
    checkImplies(LESS_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, FULFILLED, UNDETERMINED);
    checkImplies(LESS_THAN_OR_EQUAL, METHOD_EQUALS, UNDETERMINED);
    checkImplies(LESS_THAN_OR_EQUAL, NOT_METHOD_EQUALS, UNDETERMINED);
  }

  private void checkImplies(Kind tested, RelationState[] expected) {
    assertThat(expected).hasSize(Kind.values().length);
    for (Kind kind : Kind.values()) {
      checkImplies(tested, kind, expected[kind.ordinal()]);
    }
  }

  private void checkImplies(Kind hypothesisKind, Kind checkedKind, RelationState relationState) {
    checkImplies(hypothesisKind, checkedKind, relationState, relationState);
  }

  private void checkImplies(Kind hypothesisKind, Kind checkedKind, RelationState directResult, RelationState transposedResult) {
    try {
      BinaryRelation hypothesis = relation(hypothesisKind, SVa, SVb);
      BinaryRelation checked = relation(checkedKind, SVa, SVb);
      checkImplies(hypothesis, checked, directResult);
      checked = relation(checkedKind, SVb, SVa);
      checkImplies(hypothesis, checked, transposedResult);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
  private void checkImplies(BinaryRelation hypothesis, BinaryRelation checked, RelationState expected) {
    RelationState actual = hypothesis.implies(checked);
    assertThat(actual).as(hypothesis.toString() + " => " + checked.toString()).isEqualTo(expected);
  }

  @Test
  public void endlessCase() {
    List<BinaryRelation> knownRelations = ImmutableList.of(
      relation(NOT_EQUAL, SVa, SVb), // a!=b
      relation(EQUAL, SVa, SVc));// a==c
    BinaryRelation checked = new NotMethodEqualsRelation(SVb, SVa); // !b.equals(a)
    assertRelationHasState(checked, knownRelations, UNDETERMINED);
  }

  @Test
  public void transitiveEqualEqual() {
    checkTransitive(EQUAL, EQUAL, EQUAL, FULFILLED);
    checkTransitive(EQUAL, EQUAL, NOT_EQUAL, UNFULFILLED);
  }

  @Test
  public void transitiveEqualNotEqual() {
    checkTransitive(EQUAL, NOT_EQUAL, EQUAL, UNFULFILLED);
    checkTransitive(EQUAL, NOT_EQUAL, NOT_EQUAL, FULFILLED);
    checkTransitive(NOT_EQUAL, EQUAL, EQUAL, UNFULFILLED);
    checkTransitive(NOT_EQUAL, EQUAL, NOT_EQUAL, FULFILLED);
  }

  @Test
  public void transitiveEqualMethodEqual() {
    checkTransitive(EQUAL, METHOD_EQUALS, EQUAL, UNDETERMINED);
    checkTransitive(EQUAL, METHOD_EQUALS, METHOD_EQUALS, FULFILLED);
    checkTransitive(METHOD_EQUALS, EQUAL, EQUAL, UNDETERMINED);
    checkTransitive(METHOD_EQUALS, EQUAL, METHOD_EQUALS, FULFILLED);
  }

  @Test
  public void transitiveEqualGreater() {
    checkTransitive(EQUAL, GREATER_THAN, EQUAL, UNFULFILLED);
    checkTransitive(EQUAL, GREATER_THAN, GREATER_THAN, FULFILLED);
    checkTransitive(EQUAL, GREATER_THAN, NOT_EQUAL, FULFILLED);
    checkTransitive(EQUAL, GREATER_THAN, LESS_THAN_OR_EQUAL, UNFULFILLED);
    checkTransitive(GREATER_THAN, EQUAL, EQUAL, UNFULFILLED);
    checkTransitive(GREATER_THAN, EQUAL, GREATER_THAN, FULFILLED);
    checkTransitive(GREATER_THAN, EQUAL, NOT_EQUAL, FULFILLED);
    checkTransitive(GREATER_THAN, EQUAL, LESS_THAN_OR_EQUAL, UNFULFILLED);
  }

  @Test
  public void transitiveEqualGreaterOrEqual() {
    checkTransitive(EQUAL, GREATER_THAN_OR_EQUAL, EQUAL, UNDETERMINED);
    checkTransitive(EQUAL, GREATER_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL, FULFILLED);
    checkTransitive(EQUAL, GREATER_THAN_OR_EQUAL, NOT_EQUAL, UNDETERMINED);
    checkTransitive(EQUAL, GREATER_THAN_OR_EQUAL, LESS_THAN, UNFULFILLED);
    checkTransitive(GREATER_THAN_OR_EQUAL, EQUAL, EQUAL, UNDETERMINED);
    checkTransitive(GREATER_THAN_OR_EQUAL, EQUAL, GREATER_THAN_OR_EQUAL, FULFILLED);
    checkTransitive(GREATER_THAN_OR_EQUAL, EQUAL, NOT_EQUAL, UNDETERMINED);
    checkTransitive(GREATER_THAN_OR_EQUAL, EQUAL, LESS_THAN, UNFULFILLED);
  }

  @Test
  public void transitiveEqualLess() {
    checkTransitive(EQUAL, LESS_THAN, EQUAL, UNFULFILLED);
    checkTransitive(EQUAL, LESS_THAN, LESS_THAN, FULFILLED);
    checkTransitive(EQUAL, LESS_THAN, NOT_EQUAL, FULFILLED);
    checkTransitive(EQUAL, LESS_THAN, GREATER_THAN_OR_EQUAL, UNFULFILLED);
    checkTransitive(LESS_THAN, EQUAL, EQUAL, UNFULFILLED);
    checkTransitive(LESS_THAN, EQUAL, LESS_THAN, FULFILLED);
    checkTransitive(LESS_THAN, EQUAL, NOT_EQUAL, FULFILLED);
    checkTransitive(LESS_THAN, EQUAL, GREATER_THAN_OR_EQUAL, UNFULFILLED);
  }

  @Test
  public void transitiveEqualLessOrEqual() {
    checkTransitive(EQUAL, LESS_THAN_OR_EQUAL, EQUAL, UNDETERMINED);
    checkTransitive(EQUAL, LESS_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, FULFILLED);
    checkTransitive(EQUAL, LESS_THAN_OR_EQUAL, NOT_EQUAL, UNDETERMINED);
    checkTransitive(EQUAL, LESS_THAN_OR_EQUAL, GREATER_THAN, UNFULFILLED);
    checkTransitive(LESS_THAN_OR_EQUAL, EQUAL, EQUAL, UNDETERMINED);
    checkTransitive(LESS_THAN_OR_EQUAL, EQUAL, LESS_THAN_OR_EQUAL, FULFILLED);
    checkTransitive(LESS_THAN_OR_EQUAL, EQUAL, NOT_EQUAL, UNDETERMINED);
    checkTransitive(LESS_THAN_OR_EQUAL, EQUAL, GREATER_THAN, UNFULFILLED);
  }

  @Test
  public void transitiveMethodEquals() {
    checkTransitive(METHOD_EQUALS, new RelationState[]{FULFILLED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNFULFILLED, UNDETERMINED, FULFILLED, UNFULFILLED});
    checkTransitive(NOT_METHOD_EQUALS, new RelationState[]{FULFILLED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNDETERMINED, UNDETERMINED, FULFILLED, UNDETERMINED});
  }

  @Test
  public void transitiveLessThanOrEqual() {
    checkTransitive(LESS_THAN_OR_EQUAL, new RelationState[]{FULFILLED, UNDETERMINED, UNDETERMINED, UNDETERMINED, FULFILLED, FULFILLED, FULFILLED, UNDETERMINED});
    checkTransitive(LESS_THAN, new RelationState[]{FULFILLED, UNDETERMINED, UNDETERMINED, UNDETERMINED, FULFILLED, FULFILLED, FULFILLED, UNDETERMINED});
  }

  private void checkTransitive(Kind hKind1, RelationState[] expectedRelationState) {
    for (Kind kind : Kind.values()) {
      checkTransitive(hKind1, kind, hKind1, expectedRelationState[kind.ordinal()]);
    }

  }
  private void checkTransitive(Kind hKind1, Kind hkind2, Kind checkedKind, RelationState directResult) {
    try {
      BinaryRelation hypothesis1 = relation(hKind1, SVa, SVb);
      BinaryRelation hypothesis2 = relation(hkind2, SVb, SVc);
      BinaryRelation checked = relation(checkedKind, SVa, SVc);
      checkTransitive(hypothesis1, hypothesis2, checked, directResult);
      checkTransitive(hypothesis1.symmetric(), hypothesis2, checked, directResult);
      checkTransitive(hypothesis1, hypothesis2.symmetric(), checked, directResult);
      checkTransitive(hypothesis1, hypothesis2, checked.symmetric(), directResult);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void checkEquality() {
    BinaryRelation rel = relation(EQUAL, SVa, SVb);
    assertThat(rel).isNotEqualTo("");
    assertThat(rel).isEqualTo(relation(EQUAL, SVa, SVb));
    assertThat(rel).isNotEqualTo(relation(NOT_EQUAL, SVa, SVb));
    assertThat(rel).isNotEqualTo(relation(EQUAL, SVa, SVc));
  }

  @Test
  public void checkTransitiveLimit() {
    List<BinaryRelation> relations = new ArrayList<>();
    SymbolicValue first = null;
    SymbolicValue previous = null;
    SymbolicValue last = null;
    for (int i = 0; i < 300; i++) {
      last = new SymbolicValue(i);
      if (first == null) {
        first = last;
      } else if (previous != null) {
        relations.add(relation(EQUAL, previous, last));
      }
      previous = last;
    }
    try {
      relation(EQUAL, first, last).resolveState(relations);
      fail("Transitive limit was exceeded, but not detected!");
    } catch (BinaryRelation.TransitiveRelationExceededException e) {
      assertThat(e.getMessage()).contains("exceeded");
    }
  }

  @Test
  public void transitiveConjunction() {
    checkConjunction(LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL, EQUAL, FULFILLED);
    checkConjunction(LESS_THAN_OR_EQUAL, NOT_EQUAL, LESS_THAN, FULFILLED);
    checkConjunction(GREATER_THAN_OR_EQUAL, NOT_EQUAL, GREATER_THAN, FULFILLED);
    checkConjunction(LESS_THAN_OR_EQUAL, NOT_METHOD_EQUALS, LESS_THAN, FULFILLED);
    checkConjunction(GREATER_THAN_OR_EQUAL, NOT_METHOD_EQUALS, GREATER_THAN, FULFILLED);
    checkConjunction(GREATER_THAN_OR_EQUAL, METHOD_EQUALS, EQUAL, UNDETERMINED);
    checkConjunction(NOT_METHOD_EQUALS, NOT_EQUAL, LESS_THAN, UNDETERMINED);
  }

  private void checkConjunction(Kind hKind1, Kind hKind2, Kind checkedKind, RelationState directResult) {
    try {
      BinaryRelation hypothesis1 = relation(hKind1, SVa, SVb);
      BinaryRelation hypothesis2 = relation(hKind2, SVa, SVb);
      BinaryRelation checked = relation(checkedKind, SVa, SVb);
      checkTransitive(hypothesis1, hypothesis2, checked, directResult);
      checkTransitive(hypothesis2, hypothesis1, checked, directResult);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void combinedTest() {
    checkCombined(EQUAL, new Kind[]{EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, METHOD_EQUALS, null});
    checkCombined(NOT_EQUAL, new Kind[]{NOT_EQUAL, null, null, null, null, null, null, null});
    checkCombined(GREATER_THAN_OR_EQUAL, new Kind[]{GREATER_THAN_OR_EQUAL, null, GREATER_THAN, GREATER_THAN_OR_EQUAL, null, null, GREATER_THAN_OR_EQUAL, null});
    checkCombined(GREATER_THAN, new Kind[]{GREATER_THAN, null, GREATER_THAN, GREATER_THAN, null, null, GREATER_THAN, null});
    checkCombined(LESS_THAN_OR_EQUAL, new Kind[]{LESS_THAN_OR_EQUAL, null, null, null, LESS_THAN, LESS_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, null});
    checkCombined(LESS_THAN, new Kind[]{LESS_THAN, null, null, null, LESS_THAN, LESS_THAN, LESS_THAN, null});
    checkCombined(METHOD_EQUALS, new Kind[]{METHOD_EQUALS, null, null, GREATER_THAN_OR_EQUAL, null, LESS_THAN_OR_EQUAL, METHOD_EQUALS, null});
    checkCombined(NOT_METHOD_EQUALS, new Kind[]{NOT_METHOD_EQUALS, null, null, null, null, null, NOT_METHOD_EQUALS, null});
  }

  private void checkCombined(Kind tested, Kind[] expected) {
    assertThat(expected).hasSize(Kind.values().length);
    for (Kind kind : Kind.values()) {
      checkCombined(tested, kind, expected[kind.ordinal()]);
    }
  }

  private void checkCombined(Kind hKind1, Kind hKind2, Kind checkedKind) {
    try {
      BinaryRelation hypothesis1 = relation(hKind1, SVa, SVb);
      BinaryRelation hypothesis2 = relation(hKind2, SVb, SVc);
      if (checkedKind == null) {
        assertThat(hypothesis1.combineUnordered(hypothesis2)).isNull();
      } else {
        BinaryRelation checked = relation(checkedKind, SVa, SVc);
        assertThat(hypothesis1.combineUnordered(hypothesis2)).isEqualTo(checked);
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testToString() {
    assertThat(relation(EQUAL, SVa, SVb).toString()).isEqualTo("SV_0==SV_1");
  }
}
