/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
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
