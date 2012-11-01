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

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class LCSTest {

  @Test
  public void getLength() {
    assertThat(new LCS("", "").getLength()).isEqualTo(0);
    assertThat(new LCS("foo", "foo").getLength()).isEqualTo(3);
    assertThat(new LCS("foo", "__f_o_o").getLength()).isEqualTo(3);
    assertThat(new LCS("__f_o_o", "foo").getLength()).isEqualTo(3);
    assertThat(new LCS("__f_o_o", "foo").getLength()).isEqualTo(3);

    LCS lcs = new LCS("foo", "foo");
    assertThat(lcs.getLength()).isEqualTo(3);
    assertThat(lcs.getLength()).isEqualTo(3);
  }

  @Test
  public void getGroups() {
    assertThat(new LCS("", "").getGroups()).containsExactly();
    assertThat(new LCS("a", "a").getGroups()).containsExactly(new Group().append(0, 0));
    assertThat(new LCS("a", "ab").getGroups()).containsExactly(new Group().append(0, 0));
    assertThat(new LCS("a", "ba").getGroups()).containsExactly(new Group().append(0, 1));
    assertThat(new LCS("baa", "aa").getGroups()).containsExactly(new Group().append(1, 0).append(2, 1));
    assertThat(new LCS("faaaf", "fbf").getGroups()).containsExactly(new Group().append(0, 0), new Group().append(4, 2));
    assertThat(new LCS("haha", "ohha").getGroups()).containsExactly(new Group().append(0, 1), new Group().append(2, 2).append(3, 3));

    LCS lcs = new LCS("foo", "foo");
    assertThat(lcs.getGroups()).containsExactly(new Group().append(0, 0).append(1, 1).append(2, 2));
    assertThat(lcs.getGroups()).containsExactly(new Group().append(0, 0).append(1, 1).append(2, 2));
  }

  @Test
  public void foo() {
    String inputI = "assertThat(foo.size()).isEqualTo(5);";
    String inputJ = "assertThat(bar.size()).isEqualTo(10);";
    LCS lcs = new LCS(inputI, inputJ);

    List<Group> groups = lcs.getGroups();
    for (Group group : groups) {
      System.out.println("Group:");
      System.out.println(" I: " + getGroup(inputI, group.getIndexesI()));
      System.out.println(" J: " + getGroup(inputJ, group.getIndexesJ()));
      System.out.println();
    }
  }

  private String getGroup(String input, List<Integer> indexes) {
    StringBuilder sb = new StringBuilder();

    for (Integer index : indexes) {
      sb.append(input.charAt(index));
    }

    return sb.toString();
  }

}
