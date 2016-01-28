/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class BinaryRelationsTest {

  private static final int NUMBER_OF_VALUES = 5;

  private static final SymbolicValue[] values = new SymbolicValue[NUMBER_OF_VALUES];

  @BeforeClass
  public static void initialize() {
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      values[i] = new SymbolicValue(i);
    }
  }

  @Test
  public void testEqual() {
    List<BinaryRelation> constraints = new ArrayList<>();
    constraints.add(new EqualRelation(values[0], values[1]));
    constraints.add(new EqualRelation(values[0], values[2]));
    constraints.add(new EqualRelation(values[0], values[3]));
    constraints.add(new EqualRelation(values[0], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(RelationState.FULFILLED);
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNFULFILLED);
        }
      }
    }
  }

  @Test
  public void testEqualChained() {
    List<BinaryRelation> constraints = new ArrayList<>();
    constraints.add(new EqualRelation(values[0], values[1]));
    constraints.add(new EqualRelation(values[1], values[2]));
    constraints.add(new EqualRelation(values[2], values[3]));
    constraints.add(new EqualRelation(values[3], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(RelationState.FULFILLED);
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNFULFILLED);
        }
      }
    }
  }

  @Test
  public void testNotEqual() {
    List<BinaryRelation> constraints = new ArrayList<>();
    constraints.add(new NotEqualRelation(values[0], values[1]));
    constraints.add(new NotEqualRelation(values[0], values[2]));
    constraints.add(new NotEqualRelation(values[0], values[3]));
    constraints.add(new NotEqualRelation(values[0], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          if (i == 0 || j == 0) {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.FULFILLED);
          } else {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNDETERMINED);
          }
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          if (i == 0 || j == 0) {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNFULFILLED);
          } else {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNDETERMINED);
            ;
          }
        }
      }
    }
  }

  @Test
  public void testNotEqualChained() {
    List<BinaryRelation> constraints = new ArrayList<>();
    constraints.add(new NotEqualRelation(values[0], values[1]));
    constraints.add(new NotEqualRelation(values[1], values[2]));
    constraints.add(new NotEqualRelation(values[2], values[3]));
    constraints.add(new NotEqualRelation(values[3], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          if (Math.abs(j - i) == 1) {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.FULFILLED);
          } else {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNDETERMINED);
            ;
          }
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          RelationState result = relation.resolveState(constraints);
          if (Math.abs(j - i) == 1) {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNFULFILLED);
          } else {
            assertThat(result).as(relation.toString()).isEqualTo(RelationState.UNDETERMINED);
            ;
          }
        }
      }
    }
  }

  @Test
  public void a_equal_b_and_b_notEqual_c_implies_a_notEqual_c() {
    BinaryRelation a_equal_b = new EqualRelation(values[0], values[1]);
    BinaryRelation b_notEqual_c = new NotEqualRelation(values[1], values[2]);
    BinaryRelation a_notEqual_c = new NotEqualRelation(values[0], values[2]);
    RelationState result = a_notEqual_c.resolveState(ImmutableList.of(a_equal_b, b_notEqual_c));
    assertThat(result).as(a_notEqual_c.toString()).isEqualTo(RelationState.FULFILLED);
  }

  @Test
  public void a_equal_b_and_b_equal_c_implies_not_a_notEqual_c() {
    BinaryRelation a_equal_b = new EqualRelation(values[0], values[1]);
    BinaryRelation b_notEqual_c = new EqualRelation(values[1], values[2]);
    BinaryRelation a_notEqual_c = new NotEqualRelation(values[0], values[2]);
    RelationState result = a_notEqual_c.resolveState(ImmutableList.of(a_equal_b, b_notEqual_c));
    assertThat(result).as(a_notEqual_c.toString()).isEqualTo(RelationState.UNFULFILLED);
  }

  @Test
  public void a_equal_b_and_a_equal_c_implies_b_equal_c() {
    BinaryRelation a_equal_b = new EqualRelation(values[0], values[1]);
    BinaryRelation a_equal_c = new EqualRelation(values[0], values[2]);
    BinaryRelation b_equal_c = new EqualRelation(values[0], values[1]);
    RelationState result = b_equal_c.resolveState(ImmutableList.of(a_equal_b, a_equal_c));
    assertThat(result).as(b_equal_c.toString()).isEqualTo(RelationState.FULFILLED);
  }

  @Test
  public void loopRelations() {
    final List<BinaryRelation> knownRelations = ImmutableList.of(
      (BinaryRelation) new NotEqualRelation(values[0], values[1]),
      (BinaryRelation) new NotEqualRelation(values[1], values[2]),
      (BinaryRelation) new NotEqualRelation(values[2], values[3]));
    final BinaryRelation checked = new NotEqualRelation(values[0], values[3]);
    RelationState result = checked.resolveState(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(RelationState.UNDETERMINED);
  }

  @Test
  public void reversedTransitive() {
    final List<BinaryRelation> knownRelations = ImmutableList.of(
      (BinaryRelation) new EqualRelation(values[0], values[1]), // a==b
      (BinaryRelation) new EqualRelation(values[0], values[2]), // a==c
      (BinaryRelation) new EqualRelation(values[0], values[3]));// a==d
    final BinaryRelation checked = new EqualRelation(values[1], values[3]); // b==d?
    RelationState result = checked.resolveState(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(RelationState.FULFILLED);
  }

  @Test
  public void transitiveA() {
    final List<BinaryRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[1], values[2]));// b!=c
    final BinaryRelation checked = new NotEqualRelation(values[0], values[2]); // a!=c?
    RelationState result = checked.resolveState(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(RelationState.FULFILLED);
  }

  @Test
  public void transitiveB() {
    final List<BinaryRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[2], values[1]));// c!=b
    final BinaryRelation checked = new NotEqualRelation(values[0], values[2]); // a!=c?
    RelationState result = checked.resolveState(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(RelationState.FULFILLED);
  }

  @Test
  public void transitiveC() {
    final List<BinaryRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[1], values[2]));// b!=c
    final BinaryRelation checked = new EqualRelation(values[0], values[2]); // a==c?
    RelationState result = checked.resolveState(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(RelationState.UNFULFILLED);
  }

  @Test
  public void transitiveD() {
    final List<BinaryRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[2], values[1]));// c!=b
    final BinaryRelation checked = new EqualRelation(values[0], values[2]); // a==c?
    RelationState result = checked.resolveState(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(RelationState.UNFULFILLED);
  }

  @Test
  public void testInverse() {
    checkInverse(EqualRelation.class);
    checkInverse(GreaterThanOrEqualRelation.class);
    checkInverse(GreaterThanRelation.class);
    checkInverse(LessThanOrEqualRelation.class);
    checkInverse(LessThanRelation.class);
    checkInverse(MethodEqualsRelation.class);
    checkInverse(NotEqualRelation.class);
    checkInverse(NotMethodEqualsRelation.class);
  }

  private void checkInverse(Class<? extends BinaryRelation> relationClass) {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    try {
      BinaryRelation relation = createRelation(relationClass, a, b);
      BinaryRelation inverseOfInverse = relation.inverse().inverse();
      assertThat(relation.getClass()).isEqualTo(inverseOfInverse.getClass());
      assertThat(relation.leftOp).isEqualTo(inverseOfInverse.leftOp);
      assertThat(relation.rightOp).isEqualTo(inverseOfInverse.rightOp);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSymmetric() {
    checkSymmetric(EqualRelation.class);
    checkSymmetric(GreaterThanOrEqualRelation.class);
    checkSymmetric(GreaterThanRelation.class);
    checkSymmetric(LessThanOrEqualRelation.class);
    checkSymmetric(LessThanRelation.class);
    checkSymmetric(MethodEqualsRelation.class);
    checkSymmetric(NotEqualRelation.class);
    checkSymmetric(NotMethodEqualsRelation.class);
  }

  private void checkSymmetric(Class<? extends BinaryRelation> relationClass) {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    try {
      BinaryRelation relation = createRelation(relationClass, a, b);
      BinaryRelation symmetricOfSymmetric = relation.symmetric().symmetric();
      assertThat(relation.getClass()).isEqualTo(symmetricOfSymmetric.getClass());
      assertThat(relation.leftOp).isEqualTo(symmetricOfSymmetric.leftOp);
      assertThat(relation.rightOp).isEqualTo(symmetricOfSymmetric.rightOp);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private void checkImplies(Class<? extends BinaryRelation> hypothesisClass, Class<? extends BinaryRelation> checkedClass, RelationState directResult,
    RelationState transposedResult) {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    try {
      BinaryRelation hypothesis = createRelation(hypothesisClass, a, b);
      BinaryRelation checked = createRelation(checkedClass, a, b);
      checkImplies(hypothesis, checked, directResult);
      checked = createRelation(checkedClass, b, a);
      checkImplies(hypothesis, checked, transposedResult);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private void checkImplies(BinaryRelation hypothesis, BinaryRelation checked, RelationState expected) {
    RelationState actual = hypothesis.implies(checked);
    assertThat(actual).as(hypothesis.toString() + " => " + checked.toString()).isEqualTo(expected);
  }

  private void checkTransitive(Class<? extends BinaryRelation> hClass1, Class<? extends BinaryRelation> hClass2, Class<? extends BinaryRelation> checkedClass,
    RelationState directResult) {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    SymbolicValue c = values[2];
    try {
      BinaryRelation hypothesis1 = createRelation(hClass1, a, b);
      BinaryRelation hypothesis2 = createRelation(hClass2, b, c);
      BinaryRelation checked = createRelation(checkedClass, a, c);
      checkTransitive(hypothesis1, hypothesis2, checked, directResult);
      checkTransitive(hypothesis1.symmetric(), hypothesis2, checked, directResult);
      checkTransitive(hypothesis1, hypothesis2.symmetric(), checked, directResult);
      checkTransitive(hypothesis1, hypothesis2, checked.symmetric(), directResult);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private void checkConjunction(Class<? extends BinaryRelation> hClass1, Class<? extends BinaryRelation> hClass2, Class<? extends BinaryRelation> checkedClass,
    RelationState directResult) {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    try {
      BinaryRelation hypothesis1 = createRelation(hClass1, a, b);
      BinaryRelation hypothesis2 = createRelation(hClass2, a, b);
      BinaryRelation checked = createRelation(checkedClass, a, b);
      checkTransitive(hypothesis1, hypothesis2, checked, directResult);
      checkTransitive(hypothesis2, hypothesis1, checked, directResult);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private BinaryRelation createRelation(Class<? extends BinaryRelation> clazz, SymbolicValue a, SymbolicValue b) throws Exception {
    Constructor<? extends BinaryRelation> constructor = clazz.getDeclaredConstructor(SymbolicValue.class, SymbolicValue.class);
    constructor.setAccessible(true);
    return constructor.newInstance(a, b);
  }

  private void checkTransitive(BinaryRelation hypothesis1, BinaryRelation hypothesis2, BinaryRelation checked, RelationState expected) {
    List<BinaryRelation> hypotheses = ImmutableList.of(hypothesis1, hypothesis2);
    RelationState actual = checked.resolveState(hypotheses);
    assertThat(actual).as(hypothesis1.toString() + " && " + hypothesis2.toString() + " => " + checked.toString()).isEqualTo(expected);
  }

  @Test
  public void testEqualImplies() {
    checkImplies(EqualRelation.class, EqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(EqualRelation.class, NotEqualRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(EqualRelation.class, MethodEqualsRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(EqualRelation.class, NotMethodEqualsRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(EqualRelation.class, GreaterThanRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(EqualRelation.class, GreaterThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(EqualRelation.class, LessThanRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(EqualRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
  }

  @Test
  public void testNotEqualImplies() {
    checkImplies(NotEqualRelation.class, EqualRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(NotEqualRelation.class, NotEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(NotEqualRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotEqualRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotEqualRelation.class, GreaterThanRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotEqualRelation.class, GreaterThanOrEqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotEqualRelation.class, LessThanRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotEqualRelation.class, LessThanOrEqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
  }

  @Test
  public void testMethodEqualsImplies() {
    checkImplies(MethodEqualsRelation.class, EqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(MethodEqualsRelation.class, NotEqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(MethodEqualsRelation.class, MethodEqualsRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(MethodEqualsRelation.class, NotMethodEqualsRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(MethodEqualsRelation.class, GreaterThanRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(MethodEqualsRelation.class, GreaterThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(MethodEqualsRelation.class, LessThanRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(MethodEqualsRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
  }

  @Test
  public void testMethodNotEqualsImplies() {
    checkImplies(NotMethodEqualsRelation.class, EqualRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(NotMethodEqualsRelation.class, NotEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(NotMethodEqualsRelation.class, MethodEqualsRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(NotMethodEqualsRelation.class, NotMethodEqualsRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(NotMethodEqualsRelation.class, GreaterThanRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotMethodEqualsRelation.class, GreaterThanOrEqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotMethodEqualsRelation.class, LessThanRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(NotMethodEqualsRelation.class, LessThanOrEqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
  }

  @Test
  public void testGreaterThanImplies() {
    checkImplies(GreaterThanRelation.class, EqualRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(GreaterThanRelation.class, NotEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(GreaterThanRelation.class, MethodEqualsRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(GreaterThanRelation.class, NotMethodEqualsRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(GreaterThanRelation.class, GreaterThanRelation.class, RelationState.FULFILLED, RelationState.UNFULFILLED);
    checkImplies(GreaterThanRelation.class, GreaterThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.UNFULFILLED);
    checkImplies(GreaterThanRelation.class, LessThanRelation.class, RelationState.UNFULFILLED, RelationState.FULFILLED);
    checkImplies(GreaterThanRelation.class, LessThanOrEqualRelation.class, RelationState.UNFULFILLED, RelationState.FULFILLED);
  }

  @Test
  public void testGreaterThanOrEqualImplies() {
    checkImplies(GreaterThanOrEqualRelation.class, EqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(GreaterThanOrEqualRelation.class, NotEqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(GreaterThanOrEqualRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(GreaterThanOrEqualRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(GreaterThanOrEqualRelation.class, GreaterThanRelation.class, RelationState.UNDETERMINED, RelationState.UNFULFILLED);
    checkImplies(GreaterThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.UNDETERMINED);
    checkImplies(GreaterThanOrEqualRelation.class, LessThanRelation.class, RelationState.UNFULFILLED, RelationState.UNDETERMINED);
    checkImplies(GreaterThanOrEqualRelation.class, LessThanOrEqualRelation.class, RelationState.UNDETERMINED, RelationState.FULFILLED);
  }

  @Test
  public void testLessThanImplies() {
    checkImplies(LessThanRelation.class, EqualRelation.class, RelationState.UNFULFILLED, RelationState.UNFULFILLED);
    checkImplies(LessThanRelation.class, NotEqualRelation.class, RelationState.FULFILLED, RelationState.FULFILLED);
    checkImplies(LessThanRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(LessThanRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(LessThanRelation.class, GreaterThanRelation.class, RelationState.UNFULFILLED, RelationState.FULFILLED);
    checkImplies(LessThanRelation.class, GreaterThanOrEqualRelation.class, RelationState.UNFULFILLED, RelationState.FULFILLED);
    checkImplies(LessThanRelation.class, LessThanRelation.class, RelationState.FULFILLED, RelationState.UNFULFILLED);
    checkImplies(LessThanRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.UNFULFILLED);
  }

  @Test
  public void testLessThanOrEqualImplies() {
    checkImplies(LessThanOrEqualRelation.class, EqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(LessThanOrEqualRelation.class, NotEqualRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(LessThanOrEqualRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(LessThanOrEqualRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED, RelationState.UNDETERMINED);
    checkImplies(LessThanOrEqualRelation.class, GreaterThanRelation.class, RelationState.UNFULFILLED, RelationState.UNDETERMINED);
    checkImplies(LessThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, RelationState.UNDETERMINED, RelationState.FULFILLED);
    checkImplies(LessThanOrEqualRelation.class, LessThanRelation.class, RelationState.UNDETERMINED, RelationState.UNFULFILLED);
    checkImplies(LessThanOrEqualRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED, RelationState.UNDETERMINED);
  }

  @Test
  public void endlessCase() {
    final List<BinaryRelation> knownRelations = ImmutableList.of(
      new NotEqualRelation(values[0], values[1]), // a!=b
      new EqualRelation(values[0], values[2]));// a==c
    final BinaryRelation checked = new NotMethodEqualsRelation(values[1], values[0]); // !b.equals(a)
    RelationState result = checked.resolveState(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(RelationState.UNDETERMINED);
  }

  @Test
  public void transitiveEqualEqual() {
    checkTransitive(EqualRelation.class, EqualRelation.class, EqualRelation.class, RelationState.FULFILLED);
    checkTransitive(EqualRelation.class, EqualRelation.class, NotEqualRelation.class, RelationState.UNFULFILLED);
  }

  @Test
  public void transitiveEqualNotEqual() {
    checkTransitive(EqualRelation.class, NotEqualRelation.class, EqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(EqualRelation.class, NotEqualRelation.class, NotEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(NotEqualRelation.class, EqualRelation.class, EqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(NotEqualRelation.class, EqualRelation.class, NotEqualRelation.class, RelationState.FULFILLED);
  }

  @Test
  public void transitiveEqualMethodEqual() {
    checkTransitive(EqualRelation.class, MethodEqualsRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(EqualRelation.class, MethodEqualsRelation.class, MethodEqualsRelation.class, RelationState.FULFILLED);
    checkTransitive(MethodEqualsRelation.class, EqualRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(MethodEqualsRelation.class, EqualRelation.class, MethodEqualsRelation.class, RelationState.FULFILLED);
  }

  @Test
  public void transitiveEqualGreater() {
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, EqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, GreaterThanRelation.class, RelationState.FULFILLED);
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, NotEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, LessThanOrEqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, EqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, GreaterThanRelation.class, RelationState.FULFILLED);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, NotEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, LessThanOrEqualRelation.class, RelationState.UNFULFILLED);
  }

  @Test
  public void transitiveEqualGreaterOrEqual() {
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, NotEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, LessThanRelation.class, RelationState.UNFULFILLED);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, GreaterThanOrEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, NotEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, LessThanRelation.class, RelationState.UNFULFILLED);
  }

  @Test
  public void transitiveEqualLess() {
    checkTransitive(EqualRelation.class, LessThanRelation.class, EqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(EqualRelation.class, LessThanRelation.class, LessThanRelation.class, RelationState.FULFILLED);
    checkTransitive(EqualRelation.class, LessThanRelation.class, NotEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(EqualRelation.class, LessThanRelation.class, GreaterThanOrEqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(LessThanRelation.class, EqualRelation.class, EqualRelation.class, RelationState.UNFULFILLED);
    checkTransitive(LessThanRelation.class, EqualRelation.class, LessThanRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanRelation.class, EqualRelation.class, NotEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanRelation.class, EqualRelation.class, GreaterThanOrEqualRelation.class, RelationState.UNFULFILLED);
  }

  @Test
  public void transitiveEqualLessOrEqual() {
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, NotEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, GreaterThanRelation.class, RelationState.UNFULFILLED);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, NotEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, GreaterThanRelation.class, RelationState.UNFULFILLED);
  }

  @Test
  public void transitiveMethodEquals() {
    checkTransitive(MethodEqualsRelation.class, EqualRelation.class, MethodEqualsRelation.class, RelationState.FULFILLED);
    checkTransitive(MethodEqualsRelation.class, NotEqualRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(MethodEqualsRelation.class, MethodEqualsRelation.class, MethodEqualsRelation.class, RelationState.FULFILLED);
    checkTransitive(MethodEqualsRelation.class, NotMethodEqualsRelation.class, MethodEqualsRelation.class, RelationState.UNFULFILLED);
    checkTransitive(MethodEqualsRelation.class, GreaterThanOrEqualRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(MethodEqualsRelation.class, GreaterThanRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(MethodEqualsRelation.class, LessThanOrEqualRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(MethodEqualsRelation.class, LessThanRelation.class, MethodEqualsRelation.class, RelationState.UNDETERMINED);
  }

  @Test
  public void transitiveNotMethodEquals() {
    checkTransitive(NotMethodEqualsRelation.class, EqualRelation.class, NotMethodEqualsRelation.class, RelationState.FULFILLED);
    checkTransitive(NotMethodEqualsRelation.class, NotEqualRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(NotMethodEqualsRelation.class, MethodEqualsRelation.class, NotMethodEqualsRelation.class, RelationState.FULFILLED);
    checkTransitive(NotMethodEqualsRelation.class, NotMethodEqualsRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(NotMethodEqualsRelation.class, GreaterThanOrEqualRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(NotMethodEqualsRelation.class, GreaterThanRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(NotMethodEqualsRelation.class, LessThanOrEqualRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED);
    checkTransitive(NotMethodEqualsRelation.class, LessThanRelation.class, NotMethodEqualsRelation.class, RelationState.UNDETERMINED);
  }

  @Test
  public void transitiveLessThanOrEqual() {
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanOrEqualRelation.class, NotEqualRelation.class, LessThanOrEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanOrEqualRelation.class, MethodEqualsRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanOrEqualRelation.class, NotMethodEqualsRelation.class, LessThanOrEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, LessThanOrEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanOrEqualRelation.class, GreaterThanRelation.class, LessThanOrEqualRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanOrEqualRelation.class, LessThanOrEqualRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanOrEqualRelation.class, LessThanRelation.class, LessThanOrEqualRelation.class, RelationState.FULFILLED);
  }

  @Test
  public void transitiveLessThan() {
    checkTransitive(LessThanRelation.class, EqualRelation.class, LessThanRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanRelation.class, NotEqualRelation.class, LessThanRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanRelation.class, MethodEqualsRelation.class, LessThanRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanRelation.class, NotMethodEqualsRelation.class, LessThanRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanRelation.class, GreaterThanOrEqualRelation.class, LessThanRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanRelation.class, GreaterThanRelation.class, LessThanRelation.class, RelationState.UNDETERMINED);
    checkTransitive(LessThanRelation.class, LessThanOrEqualRelation.class, LessThanRelation.class, RelationState.FULFILLED);
    checkTransitive(LessThanRelation.class, LessThanRelation.class, LessThanRelation.class, RelationState.FULFILLED);
  }

  @Test
  public void checkEquality() {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    SymbolicValue c = values[2];
    BinaryRelation rel = new EqualRelation(a, b);
    assertThat(rel).isNotEqualTo(new String());
    assertThat(rel).isEqualTo(new EqualRelation(a, b));
    assertThat(rel).isNotEqualTo(new NotEqualRelation(a, b));
    assertThat(rel).isNotEqualTo(new EqualRelation(a, c));
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
        relations.add(new EqualRelation(previous, last));
      }
      previous = last;
    }
    try {
      new EqualRelation(first, last).resolveState(relations);
      Assert.fail("Transitive limit was exceeded, but not detected!");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo(BinaryRelation.TRANSITIVE_RELATIONS_EXCEEDED);
    }
  }

  @Test
  public void transitiveConjunction() {
    checkConjunction(LessThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, EqualRelation.class, RelationState.FULFILLED);
    checkConjunction(LessThanOrEqualRelation.class, NotEqualRelation.class, LessThanRelation.class, RelationState.FULFILLED);
    checkConjunction(GreaterThanOrEqualRelation.class, NotEqualRelation.class, GreaterThanRelation.class, RelationState.FULFILLED);
    checkConjunction(LessThanOrEqualRelation.class, NotMethodEqualsRelation.class, LessThanRelation.class, RelationState.FULFILLED);
    checkConjunction(GreaterThanOrEqualRelation.class, NotMethodEqualsRelation.class, GreaterThanRelation.class, RelationState.FULFILLED);
    checkConjunction(GreaterThanOrEqualRelation.class, MethodEqualsRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
    checkConjunction(NotMethodEqualsRelation.class, NotMethodEqualsRelation.class, EqualRelation.class, RelationState.UNDETERMINED);
  }
}
