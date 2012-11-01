/*
 * Sonar Java
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
package org.sonar.java.checks.codesnippet;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class VaryingGroupTest {

  @Test
  public void i() {
    assertThat(new VaryingGroup().appendI(1).appendI(2).getIndexesI()).containsExactly(1, 2);
    assertThat(new VaryingGroup().appendI(2).getIndexesI()).containsExactly(2);
  }

  @Test
  public void j() {
    assertThat(new VaryingGroup().appendJ(1).appendJ(2).getIndexesJ()).containsExactly(1, 2);
    assertThat(new VaryingGroup().appendJ(2).getIndexesJ()).containsExactly(2);
  }

  @Test
  public void isEmpty() {
    assertThat(new VaryingGroup().isEmpty()).isEqualTo(true);
    assertThat(new VaryingGroup().appendI(1).isEmpty()).isEqualTo(false);
    assertThat(new VaryingGroup().appendJ(1).isEmpty()).isEqualTo(false);
    assertThat(new VaryingGroup().appendI(1).appendJ(1).isEmpty()).isEqualTo(false);
  }

  @Test
  public void equals_test() {
    assertThat(new VaryingGroup().equals(null)).isEqualTo(false);
    assertThat(new VaryingGroup().equals(mock(Object.class))).isEqualTo(false);
    assertThat(new VaryingGroup().equals(mock(CommonGroup.class))).isEqualTo(false);
    VaryingGroup group = new VaryingGroup();
    assertThat(group.equals(group)).isEqualTo(true);
    assertThat(new VaryingGroup().equals(new VaryingGroup())).isEqualTo(true);
    assertThat(new VaryingGroup().appendI(1).appendJ(2).equals(new VaryingGroup().appendI(1).appendJ(2))).isEqualTo(true);
    assertThat(new VaryingGroup().appendI(1).appendJ(2).equals(new VaryingGroup().appendI(0).appendJ(2))).isEqualTo(false);
    assertThat(new VaryingGroup().appendI(1).appendJ(2).equals(new VaryingGroup().appendI(1).appendJ(0))).isEqualTo(false);
  }

  @Test
  public void hashCode_test() {
    assertThat(new VaryingGroup().hashCode()).isEqualTo(new VaryingGroup().hashCode());
    assertThat(new VaryingGroup().hashCode()).isNotEqualTo(new VaryingGroup().appendI(1).hashCode());
  }

  @Test
  public void toString_test() {
    assertThat(new VaryingGroup().toString()).isEqualTo("i = [], j = []");
    assertThat(new VaryingGroup().appendI(1).toString()).isEqualTo("i = [1], j = []");
    assertThat(new VaryingGroup().appendI(2).appendJ(3).appendJ(4).toString()).isEqualTo("i = [2], j = [3, 4]");
  }

}
