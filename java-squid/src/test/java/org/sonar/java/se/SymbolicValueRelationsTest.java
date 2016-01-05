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
package org.sonar.java.se;

import com.google.common.collect.ImmutableList;
import org.fest.assertions.BooleanAssert;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SymbolicValueRelationsTest {

  private static final int NUMBER_OF_VALUES = 5;

  private static final SymbolicValue[] values = new SymbolicValue[NUMBER_OF_VALUES];

  static {
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      values[i] = new SymbolicValue(i);
    }
  }

  @Test
  public void testEqual() {
    List<SymbolicValueRelation> constraints = new ArrayList<>();
    constraints.add(new EqualRelation(values[0], values[1]));
    constraints.add(new EqualRelation(values[0], values[2]));
    constraints.add(new EqualRelation(values[0], values[3]));
    constraints.add(new EqualRelation(values[0], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(Boolean.TRUE);
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(Boolean.FALSE);
        }
      }
    }
  }

  @Test
  public void testEqualChained() {
    List<SymbolicValueRelation> constraints = new ArrayList<>();
    constraints.add(new EqualRelation(values[0], values[1]));
    constraints.add(new EqualRelation(values[1], values[2]));
    constraints.add(new EqualRelation(values[2], values[3]));
    constraints.add(new EqualRelation(values[3], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(Boolean.TRUE);
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          assertThat(result).as(relation.toString()).isEqualTo(Boolean.FALSE);
        }
      }
    }
  }

  @Test
  public void testNotEqual() {
    List<SymbolicValueRelation> constraints = new ArrayList<>();
    constraints.add(new NotEqualRelation(values[0], values[1]));
    constraints.add(new NotEqualRelation(values[0], values[2]));
    constraints.add(new NotEqualRelation(values[0], values[3]));
    constraints.add(new NotEqualRelation(values[0], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          if (i == 0 || j == 0) {
            assertThat(result).as(relation.toString()).isEqualTo(Boolean.TRUE);
          } else {
            assertThat(result).as(relation.toString()).isNull();
          }
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          if (i == 0 || j == 0) {
            assertThat(result).as(relation.toString()).isEqualTo(Boolean.FALSE);
          } else {
            assertThat(result).as(relation.toString()).isNull();
            ;
          }
        }
      }
    }
  }

  @Test
  public void testNotEqualChained() {
    List<SymbolicValueRelation> constraints = new ArrayList<>();
    constraints.add(new NotEqualRelation(values[0], values[1]));
    constraints.add(new NotEqualRelation(values[1], values[2]));
    constraints.add(new NotEqualRelation(values[2], values[3]));
    constraints.add(new NotEqualRelation(values[3], values[4]));
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final NotEqualRelation relation = new NotEqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          if (Math.abs(j - i) == 1) {
            assertThat(result).as(relation.toString()).isEqualTo(Boolean.TRUE);
          } else {
            assertThat(result).as(relation.toString()).isNull();
            ;
          }
        }
      }
    }
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      for (int j = 0; j < NUMBER_OF_VALUES; j++) {
        if (i != j) {
          final EqualRelation relation = new EqualRelation(values[i], values[j]);
          Boolean result = relation.impliedBy(constraints);
          if (Math.abs(j - i) == 1) {
            assertThat(result).as(relation.toString()).isEqualTo(Boolean.FALSE);
          } else {
            assertThat(result).as(relation.toString()).isNull();
            ;
          }
        }
      }
    }
  }

  @Test
  public void a_equal_b_and_b_notEqual_c_implies_a_notEqual_c() {
    SymbolicValueRelation a_equal_b = new EqualRelation(values[0], values[1]);
    SymbolicValueRelation b_notEqual_c = new NotEqualRelation(values[1], values[2]);
    SymbolicValueRelation a_notEqual_c = new NotEqualRelation(values[0], values[2]);
    Boolean result = a_notEqual_c.impliedBy(ImmutableList.of(a_equal_b, b_notEqual_c));
    assertThat(result).as(a_notEqual_c.toString()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void a_equal_b_and_b_equal_c_implies_not_a_notEqual_c() {
    SymbolicValueRelation a_equal_b = new EqualRelation(values[0], values[1]);
    SymbolicValueRelation b_notEqual_c = new EqualRelation(values[1], values[2]);
    SymbolicValueRelation a_notEqual_c = new NotEqualRelation(values[0], values[2]);
    Boolean result = a_notEqual_c.impliedBy(ImmutableList.of(a_equal_b, b_notEqual_c));
    assertThat(result).as(a_notEqual_c.toString()).isEqualTo(Boolean.FALSE);
  }

  @Test
  public void a_equal_b_and_a_equal_c_implies_b_equal_c() {
    SymbolicValueRelation a_equal_b = new EqualRelation(values[0], values[1]);
    SymbolicValueRelation a_equal_c = new EqualRelation(values[0], values[2]);
    SymbolicValueRelation b_equal_c = new EqualRelation(values[0], values[1]);
    Boolean result = b_equal_c.impliedBy(ImmutableList.of(a_equal_b, a_equal_c));
    assertThat(result).as(b_equal_c.toString()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void loopRelations() {
    final List<SymbolicValueRelation> knownRelations = ImmutableList.of(
      (SymbolicValueRelation) new NotEqualRelation(values[0], values[1]),
      (SymbolicValueRelation) new NotEqualRelation(values[1], values[2]),
      (SymbolicValueRelation) new NotEqualRelation(values[2], values[3]));
    final SymbolicValueRelation checked = new NotEqualRelation(values[0], values[3]);
    Boolean result = checked.impliedBy(knownRelations);
    assertThat(result).as(checked.toString()).isNull();
  }

  @Test
  public void reversedTransitive() {
    final List<SymbolicValueRelation> knownRelations = ImmutableList.of(
      (SymbolicValueRelation) new EqualRelation(values[0], values[1]), // a==b
      (SymbolicValueRelation) new EqualRelation(values[0], values[2]), // a==c
      (SymbolicValueRelation) new EqualRelation(values[0], values[3]));// a==d
    final SymbolicValueRelation checked = new EqualRelation(values[1], values[3]); // b==d?
    Boolean result = checked.impliedBy(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void transitiveA() {
    final List<SymbolicValueRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[1], values[2]));// b!=c
    final SymbolicValueRelation checked = new NotEqualRelation(values[0], values[2]); // a!=c?
    Boolean result = checked.impliedBy(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void transitiveB() {
    final List<SymbolicValueRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[2], values[1]));// c!=b
    final SymbolicValueRelation checked = new NotEqualRelation(values[0], values[2]); // a!=c?
    Boolean result = checked.impliedBy(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void transitiveC() {
    final List<SymbolicValueRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[1], values[2]));// b!=c
    final SymbolicValueRelation checked = new EqualRelation(values[0], values[2]); // a==c?
    Boolean result = checked.impliedBy(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(Boolean.FALSE);
  }

  @Test
  public void transitiveD() {
    final List<SymbolicValueRelation> knownRelations = ImmutableList.of(
      new EqualRelation(values[0], values[1]), // a==b
      new NotEqualRelation(values[2], values[1]));// c!=b
    final SymbolicValueRelation checked = new EqualRelation(values[0], values[2]); // a==c?
    Boolean result = checked.impliedBy(knownRelations);
    assertThat(result).as(checked.toString()).isEqualTo(Boolean.FALSE);
  }

  private void checkImplies(Class<? extends SymbolicValueRelation> hypothesisClass, Class<? extends SymbolicValueRelation> checkedClass, Boolean directResult,
    Boolean transposedResult) {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    try {
      SymbolicValueRelation hypothesis = hypothesisClass.getConstructor(SymbolicValue.class, SymbolicValue.class).newInstance(a, b);
      SymbolicValueRelation checked = checkedClass.getConstructor(SymbolicValue.class, SymbolicValue.class).newInstance(a, b);
      checkImplies(hypothesis, checked, directResult);
      checked = checkedClass.getConstructor(SymbolicValue.class, SymbolicValue.class).newInstance(b, a);
      checkImplies(hypothesis, checked, transposedResult);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private void checkImplies(SymbolicValueRelation hypothesis, SymbolicValueRelation checked, Boolean result) {
    final BooleanAssert assertion = assertThat(hypothesis.implies(checked)).as(hypothesis.toString() + " => " + checked.toString());
    if (result == null) {
      assertion.isNull();
    } else {
      assertion.isEqualTo(result);
    }
  }

  private void checkTransitive(Class<? extends SymbolicValueRelation> hClass1, Class<? extends SymbolicValueRelation> hClass2, Class<? extends SymbolicValueRelation> checkedClass,
    Boolean directResult) {
    SymbolicValue a = values[0];
    SymbolicValue b = values[1];
    SymbolicValue c = values[2];
    try {
      SymbolicValueRelation hypothesis1 = hClass1.getConstructor(SymbolicValue.class, SymbolicValue.class).newInstance(a, b);
      SymbolicValueRelation hypothesis2 = hClass2.getConstructor(SymbolicValue.class, SymbolicValue.class).newInstance(b, c);
      SymbolicValueRelation checked = checkedClass.getConstructor(SymbolicValue.class, SymbolicValue.class).newInstance(a, c);
      checkTransitive(hypothesis1, hypothesis2, checked, directResult);
      checkTransitive(hypothesis1.symmetric(), hypothesis2, checked, directResult);
      checkTransitive(hypothesis1, hypothesis2.symmetric(), checked, directResult);
      checkTransitive(hypothesis1, hypothesis2, checked.symmetric(), directResult);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private void checkTransitive(SymbolicValueRelation hypothesis1, SymbolicValueRelation hypothesis2, SymbolicValueRelation checked, Boolean result) {
    List<SymbolicValueRelation> hypotheses = ImmutableList.of(hypothesis1, hypothesis2);
    final BooleanAssert assertion = assertThat(checked.impliedBy(hypotheses)).as(hypothesis1.toString() + " && " + hypothesis2.toString() + " => " + checked.toString());
    if (result == null) {
      assertion.isNull();
    } else {
      assertion.isEqualTo(result);
    }
  }

  @Test
  public void testEqualImplies() {
    checkImplies(EqualRelation.class, EqualRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(EqualRelation.class, NotEqualRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(EqualRelation.class, MethodEqualsRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(EqualRelation.class, NotMethodEqualsRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(EqualRelation.class, GreaterThanRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(EqualRelation.class, GreaterThanOrEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(EqualRelation.class, LessThanRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(EqualRelation.class, LessThanOrEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
  }

  @Test
  public void testNotEqualImplies() {
    checkImplies(NotEqualRelation.class, EqualRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(NotEqualRelation.class, NotEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(NotEqualRelation.class, MethodEqualsRelation.class, null, null);
    checkImplies(NotEqualRelation.class, NotMethodEqualsRelation.class, null, null);
    checkImplies(NotEqualRelation.class, GreaterThanRelation.class, null, null);
    checkImplies(NotEqualRelation.class, GreaterThanOrEqualRelation.class, null, null);
    checkImplies(NotEqualRelation.class, LessThanRelation.class, null, null);
    checkImplies(NotEqualRelation.class, LessThanOrEqualRelation.class, null, null);
  }

  @Test
  public void testMethodEqualsImplies() {
    checkImplies(MethodEqualsRelation.class, EqualRelation.class, null, null);
    checkImplies(MethodEqualsRelation.class, NotEqualRelation.class, null, null);
    checkImplies(MethodEqualsRelation.class, MethodEqualsRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(MethodEqualsRelation.class, NotMethodEqualsRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(MethodEqualsRelation.class, GreaterThanRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(MethodEqualsRelation.class, GreaterThanOrEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(MethodEqualsRelation.class, LessThanRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(MethodEqualsRelation.class, LessThanOrEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
  }

  @Test
  public void testMethodNotEqualsImplies() {
    checkImplies(NotMethodEqualsRelation.class, EqualRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(NotMethodEqualsRelation.class, NotEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(NotMethodEqualsRelation.class, MethodEqualsRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(NotMethodEqualsRelation.class, NotMethodEqualsRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(NotMethodEqualsRelation.class, GreaterThanRelation.class, null, null);
    checkImplies(NotMethodEqualsRelation.class, GreaterThanOrEqualRelation.class, null, null);
    checkImplies(NotMethodEqualsRelation.class, LessThanRelation.class, null, null);
    checkImplies(NotMethodEqualsRelation.class, LessThanOrEqualRelation.class, null, null);
  }

  @Test
  public void testGreaterThanImplies() {
    checkImplies(GreaterThanRelation.class, EqualRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(GreaterThanRelation.class, NotEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(GreaterThanRelation.class, MethodEqualsRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(GreaterThanRelation.class, NotMethodEqualsRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(GreaterThanRelation.class, GreaterThanRelation.class, Boolean.TRUE, Boolean.FALSE);
    checkImplies(GreaterThanRelation.class, GreaterThanOrEqualRelation.class, Boolean.TRUE, Boolean.FALSE);
    checkImplies(GreaterThanRelation.class, LessThanRelation.class, Boolean.FALSE, Boolean.TRUE);
    checkImplies(GreaterThanRelation.class, LessThanOrEqualRelation.class, Boolean.FALSE, Boolean.TRUE);
  }

  @Test
  public void testGreaterThanOrEqualImplies() {
    checkImplies(GreaterThanOrEqualRelation.class, EqualRelation.class, null, null);
    checkImplies(GreaterThanOrEqualRelation.class, NotEqualRelation.class, null, null);
    checkImplies(GreaterThanOrEqualRelation.class, MethodEqualsRelation.class, null, null);
    checkImplies(GreaterThanOrEqualRelation.class, NotMethodEqualsRelation.class, null, null);
    checkImplies(GreaterThanOrEqualRelation.class, GreaterThanRelation.class, null, Boolean.FALSE);
    checkImplies(GreaterThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, Boolean.TRUE, null);
    checkImplies(GreaterThanOrEqualRelation.class, LessThanRelation.class, Boolean.FALSE, null);
    checkImplies(GreaterThanOrEqualRelation.class, LessThanOrEqualRelation.class, null, Boolean.TRUE);
  }

  @Test
  public void testLessThanImplies() {
    checkImplies(LessThanRelation.class, EqualRelation.class, Boolean.FALSE, Boolean.FALSE);
    checkImplies(LessThanRelation.class, NotEqualRelation.class, Boolean.TRUE, Boolean.TRUE);
    checkImplies(LessThanRelation.class, MethodEqualsRelation.class, null, null);
    checkImplies(LessThanRelation.class, NotMethodEqualsRelation.class, null, null);
    checkImplies(LessThanRelation.class, GreaterThanRelation.class, Boolean.FALSE, Boolean.TRUE);
    checkImplies(LessThanRelation.class, GreaterThanOrEqualRelation.class, Boolean.FALSE, Boolean.TRUE);
    checkImplies(LessThanRelation.class, LessThanRelation.class, Boolean.TRUE, Boolean.FALSE);
    checkImplies(LessThanRelation.class, LessThanOrEqualRelation.class, Boolean.TRUE, Boolean.FALSE);
  }

  @Test
  public void testLessThanOrEqualImplies() {
    checkImplies(LessThanOrEqualRelation.class, EqualRelation.class, null, null);
    checkImplies(LessThanOrEqualRelation.class, NotEqualRelation.class, null, null);
    checkImplies(LessThanOrEqualRelation.class, MethodEqualsRelation.class, null, null);
    checkImplies(LessThanOrEqualRelation.class, NotMethodEqualsRelation.class, null, null);
    checkImplies(LessThanOrEqualRelation.class, GreaterThanRelation.class, Boolean.FALSE, null);
    checkImplies(LessThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, null, Boolean.TRUE);
    checkImplies(LessThanOrEqualRelation.class, LessThanRelation.class, null, Boolean.FALSE);
    checkImplies(LessThanOrEqualRelation.class, LessThanOrEqualRelation.class, Boolean.TRUE, null);
  }

  @Test
  public void endlessCase() {
    final List<SymbolicValueRelation> knownRelations = ImmutableList.of(
      new NotEqualRelation(values[0], values[1]), // a!=b
      new EqualRelation(values[0], values[2]));// a==c
    final SymbolicValueRelation checked = new NotMethodEqualsRelation(values[1], values[0]); // !b.equals(a)
    Boolean result = checked.impliedBy(knownRelations);
    assertThat(result).as(checked.toString()).isNull();
  }

  @Test
  public void transitiveEqualEqual() {
    checkTransitive(EqualRelation.class, EqualRelation.class, EqualRelation.class, Boolean.TRUE);
    checkTransitive(EqualRelation.class, EqualRelation.class, NotEqualRelation.class, Boolean.FALSE);
  }

  @Test
  public void transitiveEqualNotEqual() {
    checkTransitive(EqualRelation.class, NotEqualRelation.class, EqualRelation.class, Boolean.FALSE);
    checkTransitive(EqualRelation.class, NotEqualRelation.class, NotEqualRelation.class, Boolean.TRUE);
    checkTransitive(NotEqualRelation.class, EqualRelation.class, EqualRelation.class, Boolean.FALSE);
    checkTransitive(NotEqualRelation.class, EqualRelation.class, NotEqualRelation.class, Boolean.TRUE);
  }

  @Test
  public void transitiveEqualMethodEqual() {
    checkTransitive(EqualRelation.class, MethodEqualsRelation.class, EqualRelation.class, null);
    checkTransitive(EqualRelation.class, MethodEqualsRelation.class, MethodEqualsRelation.class, Boolean.TRUE);
    checkTransitive(MethodEqualsRelation.class, EqualRelation.class, EqualRelation.class, null);
    checkTransitive(MethodEqualsRelation.class, EqualRelation.class, MethodEqualsRelation.class, Boolean.TRUE);
  }

  @Test
  public void transitiveEqualGreater() {
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, EqualRelation.class, Boolean.FALSE);
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, GreaterThanRelation.class, Boolean.TRUE);
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, NotEqualRelation.class, Boolean.TRUE);
    checkTransitive(EqualRelation.class, GreaterThanRelation.class, LessThanOrEqualRelation.class, Boolean.FALSE);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, EqualRelation.class, Boolean.FALSE);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, GreaterThanRelation.class, Boolean.TRUE);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, NotEqualRelation.class, Boolean.TRUE);
    checkTransitive(GreaterThanRelation.class, EqualRelation.class, LessThanOrEqualRelation.class, Boolean.FALSE);
  }

  @Test
  public void transitiveEqualGreaterOrEqual() {
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, EqualRelation.class, null);
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, GreaterThanOrEqualRelation.class, Boolean.TRUE);
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, NotEqualRelation.class, null);
    checkTransitive(EqualRelation.class, GreaterThanOrEqualRelation.class, LessThanRelation.class, Boolean.FALSE);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, EqualRelation.class, null);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, GreaterThanOrEqualRelation.class, Boolean.TRUE);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, NotEqualRelation.class, null);
    checkTransitive(GreaterThanOrEqualRelation.class, EqualRelation.class, LessThanRelation.class, Boolean.FALSE);
  }

  @Test
  public void transitiveEqualLess() {
    checkTransitive(EqualRelation.class, LessThanRelation.class, EqualRelation.class, Boolean.FALSE);
    checkTransitive(EqualRelation.class, LessThanRelation.class, LessThanRelation.class, Boolean.TRUE);
    checkTransitive(EqualRelation.class, LessThanRelation.class, NotEqualRelation.class, Boolean.TRUE);
    checkTransitive(EqualRelation.class, LessThanRelation.class, GreaterThanOrEqualRelation.class, Boolean.FALSE);
    checkTransitive(LessThanRelation.class, EqualRelation.class, EqualRelation.class, Boolean.FALSE);
    checkTransitive(LessThanRelation.class, EqualRelation.class, LessThanRelation.class, Boolean.TRUE);
    checkTransitive(LessThanRelation.class, EqualRelation.class, NotEqualRelation.class, Boolean.TRUE);
    checkTransitive(LessThanRelation.class, EqualRelation.class, GreaterThanOrEqualRelation.class, Boolean.FALSE);
  }

  @Test
  public void transitiveEqualLessOrEqual() {
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, EqualRelation.class, null);
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, LessThanOrEqualRelation.class, Boolean.TRUE);
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, NotEqualRelation.class, null);
    checkTransitive(EqualRelation.class, LessThanOrEqualRelation.class, GreaterThanRelation.class, Boolean.FALSE);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, EqualRelation.class, null);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, LessThanOrEqualRelation.class, Boolean.TRUE);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, NotEqualRelation.class, null);
    checkTransitive(LessThanOrEqualRelation.class, EqualRelation.class, GreaterThanRelation.class, Boolean.FALSE);
  }
}
