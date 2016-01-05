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
}
