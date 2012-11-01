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

public class GroupTest {

  @Test
  public void i() {
    assertThat(new Group().prepend(1, 0).prepend(2, 0).getIndexesI()).containsExactly(2, 1);
    assertThat(new Group().append(1, 0).append(2, 0).getIndexesI()).containsExactly(1, 2);
  }

  @Test
  public void j() {
    assertThat(new Group().prepend(0, 1).prepend(0, 2).getIndexesJ()).containsExactly(2, 1);
    assertThat(new Group().append(0, 1).append(0, 2).getIndexesJ()).containsExactly(1, 2);
  }

  @Test
  public void equals_test() {
    assertThat(new Group().equals(null)).isEqualTo(false);
    assertThat(new Group().equals(mock(Object.class))).isEqualTo(false);
    Group group = new Group();
    assertThat(group.equals(group)).isEqualTo(true);
    assertThat(new Group().equals(new Group())).isEqualTo(true);
    assertThat(new Group().append(1, 2).equals(new Group().append(1, 2))).isEqualTo(true);
    assertThat(new Group().append(1, 2).equals(new Group().append(1, 0))).isEqualTo(false);
    assertThat(new Group().append(1, 2).equals(new Group().append(0, 2))).isEqualTo(false);
  }

  @Test
  public void hashCode_test() {
    assertThat(new Group().hashCode()).isEqualTo(new Group().hashCode());
    assertThat(new Group().hashCode()).isNotEqualTo(new Group().append(1, 2).hashCode());
  }

  @Test
  public void toString_test() {
    assertThat(new Group().toString()).isEqualTo("i = [], j = []");
    assertThat(new Group().append(1, 2).toString()).isEqualTo("i = [1], j = [2]");
    assertThat(new Group().append(1, 2).append(2, 3).toString()).isEqualTo("i = [1, 2], j = [2, 3]");
  }

}
