/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.symexec;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class SymbolicRelationTest {

  @Test
  public void test_negate() {
    assertThat(SymbolicRelation.EQUAL_TO.negate()).isSameAs(SymbolicRelation.NOT_EQUAL);
    assertThat(SymbolicRelation.GREATER_EQUAL.negate()).isSameAs(SymbolicRelation.LESS_THAN);
    assertThat(SymbolicRelation.GREATER_THAN.negate()).isSameAs(SymbolicRelation.LESS_EQUAL);
    assertThat(SymbolicRelation.LESS_EQUAL.negate()).isSameAs(SymbolicRelation.GREATER_THAN);
    assertThat(SymbolicRelation.LESS_THAN.negate()).isSameAs(SymbolicRelation.GREATER_EQUAL);
    assertThat(SymbolicRelation.NOT_EQUAL.negate()).isSameAs(SymbolicRelation.EQUAL_TO);
    assertThat(SymbolicRelation.UNKNOWN.negate()).isSameAs(SymbolicRelation.UNKNOWN);
  }

  @Test
  public void test_swap() {
    assertThat(SymbolicRelation.EQUAL_TO.swap()).isSameAs(SymbolicRelation.EQUAL_TO);
    assertThat(SymbolicRelation.GREATER_EQUAL.swap()).isSameAs(SymbolicRelation.LESS_EQUAL);
    assertThat(SymbolicRelation.GREATER_THAN.swap()).isSameAs(SymbolicRelation.LESS_THAN);
    assertThat(SymbolicRelation.LESS_EQUAL.swap()).isSameAs(SymbolicRelation.GREATER_EQUAL);
    assertThat(SymbolicRelation.LESS_THAN.swap()).isSameAs(SymbolicRelation.GREATER_THAN);
    assertThat(SymbolicRelation.NOT_EQUAL.swap()).isSameAs(SymbolicRelation.NOT_EQUAL);
    assertThat(SymbolicRelation.UNKNOWN.swap()).isSameAs(SymbolicRelation.UNKNOWN);
  }

}
