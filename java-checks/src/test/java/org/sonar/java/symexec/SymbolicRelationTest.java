/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import static org.sonar.java.symexec.SymbolicRelation.EQUAL_TO;
import static org.sonar.java.symexec.SymbolicRelation.GREATER_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.GREATER_THAN;
import static org.sonar.java.symexec.SymbolicRelation.LESS_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.LESS_THAN;
import static org.sonar.java.symexec.SymbolicRelation.NOT_EQUAL;
import static org.sonar.java.symexec.SymbolicRelation.UNKNOWN;

public class SymbolicRelationTest {

  @Test
  public void test_negate() {
    assertThat(EQUAL_TO.negate()).isSameAs(NOT_EQUAL);
    assertThat(GREATER_EQUAL.negate()).isSameAs(LESS_THAN);
    assertThat(GREATER_THAN.negate()).isSameAs(LESS_EQUAL);
    assertThat(LESS_EQUAL.negate()).isSameAs(GREATER_THAN);
    assertThat(LESS_THAN.negate()).isSameAs(GREATER_EQUAL);
    assertThat(NOT_EQUAL.negate()).isSameAs(EQUAL_TO);
    assertThat(UNKNOWN.negate()).isSameAs(UNKNOWN);
  }

  @Test
  public void test_swap() {
    assertThat(EQUAL_TO.swap()).isSameAs(EQUAL_TO);
    assertThat(GREATER_EQUAL.swap()).isSameAs(LESS_EQUAL);
    assertThat(GREATER_THAN.swap()).isSameAs(LESS_THAN);
    assertThat(LESS_EQUAL.swap()).isSameAs(GREATER_EQUAL);
    assertThat(LESS_THAN.swap()).isSameAs(GREATER_THAN);
    assertThat(NOT_EQUAL.swap()).isSameAs(NOT_EQUAL);
    assertThat(UNKNOWN.swap()).isSameAs(UNKNOWN);
  }

  @Test
  public void test_union() {
    assertThat(EQUAL_TO.union(null)).isSameAs(EQUAL_TO);
    assertThat(EQUAL_TO.union(EQUAL_TO)).isSameAs(EQUAL_TO);
    assertThat(EQUAL_TO.union(GREATER_EQUAL)).isSameAs(GREATER_EQUAL);
    assertThat(EQUAL_TO.union(GREATER_THAN)).isSameAs(GREATER_EQUAL);
    assertThat(EQUAL_TO.union(LESS_EQUAL)).isSameAs(LESS_EQUAL);
    assertThat(EQUAL_TO.union(LESS_THAN)).isSameAs(LESS_EQUAL);
    assertThat(EQUAL_TO.union(NOT_EQUAL)).isSameAs(UNKNOWN);
    assertThat(EQUAL_TO.union(UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(GREATER_EQUAL.union(null)).isSameAs(GREATER_EQUAL);
    assertThat(GREATER_EQUAL.union(EQUAL_TO)).isSameAs(GREATER_EQUAL);
    assertThat(GREATER_EQUAL.union(GREATER_EQUAL)).isSameAs(GREATER_EQUAL);
    assertThat(GREATER_EQUAL.union(GREATER_THAN)).isSameAs(GREATER_EQUAL);
    assertThat(GREATER_EQUAL.union(LESS_EQUAL)).isSameAs(UNKNOWN);
    assertThat(GREATER_EQUAL.union(LESS_THAN)).isSameAs(UNKNOWN);
    assertThat(GREATER_EQUAL.union(NOT_EQUAL)).isSameAs(UNKNOWN);
    assertThat(GREATER_EQUAL.union(UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(GREATER_THAN.union(null)).isSameAs(GREATER_THAN);
    assertThat(GREATER_THAN.union(EQUAL_TO)).isSameAs(GREATER_EQUAL);
    assertThat(GREATER_THAN.union(GREATER_EQUAL)).isSameAs(GREATER_EQUAL);
    assertThat(GREATER_THAN.union(GREATER_THAN)).isSameAs(GREATER_THAN);
    assertThat(GREATER_THAN.union(LESS_EQUAL)).isSameAs(UNKNOWN);
    assertThat(GREATER_THAN.union(LESS_THAN)).isSameAs(NOT_EQUAL);
    assertThat(GREATER_THAN.union(NOT_EQUAL)).isSameAs(NOT_EQUAL);
    assertThat(GREATER_THAN.union(UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(LESS_EQUAL.union(null)).isSameAs(LESS_EQUAL);
    assertThat(LESS_EQUAL.union(EQUAL_TO)).isSameAs(LESS_EQUAL);
    assertThat(LESS_EQUAL.union(GREATER_EQUAL)).isSameAs(UNKNOWN);
    assertThat(LESS_EQUAL.union(GREATER_THAN)).isSameAs(UNKNOWN);
    assertThat(LESS_EQUAL.union(LESS_EQUAL)).isSameAs(LESS_EQUAL);
    assertThat(LESS_EQUAL.union(LESS_THAN)).isSameAs(LESS_EQUAL);
    assertThat(LESS_EQUAL.union(NOT_EQUAL)).isSameAs(UNKNOWN);
    assertThat(LESS_EQUAL.union(UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(LESS_THAN.union(null)).isSameAs(LESS_THAN);
    assertThat(LESS_THAN.union(EQUAL_TO)).isSameAs(LESS_EQUAL);
    assertThat(LESS_THAN.union(GREATER_EQUAL)).isSameAs(UNKNOWN);
    assertThat(LESS_THAN.union(GREATER_THAN)).isSameAs(NOT_EQUAL);
    assertThat(LESS_THAN.union(LESS_EQUAL)).isSameAs(LESS_EQUAL);
    assertThat(LESS_THAN.union(LESS_THAN)).isSameAs(LESS_THAN);
    assertThat(LESS_THAN.union(NOT_EQUAL)).isSameAs(NOT_EQUAL);
    assertThat(LESS_THAN.union(UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(NOT_EQUAL.union(null)).isSameAs(NOT_EQUAL);
    assertThat(NOT_EQUAL.union(EQUAL_TO)).isSameAs(UNKNOWN);
    assertThat(NOT_EQUAL.union(GREATER_EQUAL)).isSameAs(UNKNOWN);
    assertThat(NOT_EQUAL.union(GREATER_THAN)).isSameAs(NOT_EQUAL);
    assertThat(NOT_EQUAL.union(LESS_EQUAL)).isSameAs(UNKNOWN);
    assertThat(NOT_EQUAL.union(LESS_THAN)).isSameAs(NOT_EQUAL);
    assertThat(NOT_EQUAL.union(NOT_EQUAL)).isSameAs(NOT_EQUAL);
    assertThat(NOT_EQUAL.union(UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(UNKNOWN.union(null)).isSameAs(UNKNOWN);
    assertThat(UNKNOWN.union(EQUAL_TO)).isSameAs(UNKNOWN);
    assertThat(UNKNOWN.union(GREATER_EQUAL)).isSameAs(UNKNOWN);
    assertThat(UNKNOWN.union(GREATER_THAN)).isSameAs(UNKNOWN);
    assertThat(UNKNOWN.union(LESS_EQUAL)).isSameAs(UNKNOWN);
    assertThat(UNKNOWN.union(LESS_THAN)).isSameAs(UNKNOWN);
    assertThat(UNKNOWN.union(NOT_EQUAL)).isSameAs(UNKNOWN);
    assertThat(UNKNOWN.union(UNKNOWN)).isSameAs(UNKNOWN);
  }

}
