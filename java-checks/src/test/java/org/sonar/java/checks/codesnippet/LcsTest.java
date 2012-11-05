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

public class LcsTest {

  @Test
  public void getLength() {
    assertThat(newLcs("", "").getLength()).isEqualTo(0);
    assertThat(newLcs("foo", "foo").getLength()).isEqualTo(3);
    assertThat(newLcs("foo", "__f_o_o").getLength()).isEqualTo(3);
    assertThat(newLcs("__f_o_o", "foo").getLength()).isEqualTo(3);
    assertThat(newLcs("__f_o_o", "foo").getLength()).isEqualTo(3);

    Lcs<Character> lcs = newLcs("foo", "foo");
    assertThat(lcs.getLength()).isEqualTo(3);
    assertThat(lcs.getLength()).isEqualTo(3);
  }

  @Test
  public void getCommonGroups() {
    assertThat(newLcs("", "").getCommonGroups()).containsExactly();
    assertThat(newLcs("a", "a").getCommonGroups()).containsExactly(new CommonGroup().append(0, 0));
    assertThat(newLcs("a", "ab").getCommonGroups()).containsExactly(new CommonGroup().append(0, 0));
    assertThat(newLcs("a", "ba").getCommonGroups()).containsExactly(new CommonGroup().append(0, 1));
    assertThat(newLcs("baa", "aa").getCommonGroups()).containsExactly(new CommonGroup().append(1, 0).append(2, 1));
    assertThat(newLcs("faaaf", "fbf").getCommonGroups()).containsExactly(new CommonGroup().append(0, 0), new CommonGroup().append(4, 2));
    assertThat(newLcs("haha", "ohha").getCommonGroups()).containsExactly(new CommonGroup().append(0, 1), new CommonGroup().append(2, 2).append(3, 3));

    Lcs<Character> lcs = newLcs("foo", "foo");
    assertThat(lcs.getCommonGroups()).containsExactly(new CommonGroup().append(0, 0).append(1, 1).append(2, 2));
    assertThat(lcs.getCommonGroups()).containsExactly(new CommonGroup().append(0, 0).append(1, 1).append(2, 2));
  }

  @Test
  public void getCommonGroups_should_always_prefer_longest_varying_group() {
    assertThat(newLcs("a(b())", "a(f)").getCommonGroups()).containsExactly(new CommonGroup().append(0, 0).append(1, 1), new CommonGroup().append(5, 3));
    assertThat(newLcs("a(f)", "a(b())").getCommonGroups()).containsExactly(new CommonGroup().append(0, 0).append(1, 1), new CommonGroup().append(3, 5));
    assertThat(newLcs("a(b()).c(d())", "a(f).c(e)").getCommonGroups()).containsExactly(
        new CommonGroup().append(0, 0).append(1, 1),
        new CommonGroup().append(5, 3).append(6, 4).append(7, 5).append(8, 6),
        new CommonGroup().append(12, 8));
    assertThat(newLcs("a(b()).c(d)", "a(f).c(e())").getCommonGroups()).containsExactly(
        new CommonGroup().append(0, 0).append(1, 1),
        new CommonGroup().append(5, 3).append(6, 4).append(7, 5).append(8, 6),
        new CommonGroup().append(10, 10));
  }

  @Test
  public void getVaryingGroups() {
    assertThat(newLcs("", "").getVaryingGroups()).containsExactly();
    assertThat(newLcs("a", "a").getVaryingGroups()).containsExactly();
    assertThat(newLcs("ab", "a").getVaryingGroups()).containsExactly(new VaryingGroup().appendI(1));
    assertThat(newLcs("a", "ab").getVaryingGroups()).containsExactly(new VaryingGroup().appendJ(1));
    assertThat(newLcs("ba", "a").getVaryingGroups()).containsExactly(new VaryingGroup().appendI(0));
    assertThat(newLcs("a", "ba").getVaryingGroups()).containsExactly(new VaryingGroup().appendJ(0));

    Lcs<Character> lcs = newLcs("abc", "b");
    assertThat(lcs.getVaryingGroups()).containsExactly(new VaryingGroup().appendI(0), new VaryingGroup().appendI(2));
    assertThat(lcs.getVaryingGroups()).containsExactly(new VaryingGroup().appendI(0), new VaryingGroup().appendI(2));
  }

  @Test
  public void getGroups() {
    assertThat(newLcs("", "").getGroups()).containsExactly();
    assertThat(newLcs("a", "a").getGroups()).containsExactly(new CommonGroup().append(0, 0));
    assertThat(newLcs("ab", "a").getGroups()).containsExactly(new CommonGroup().append(0, 0), new VaryingGroup().appendI(1));
    assertThat(newLcs("ba", "a").getGroups()).containsExactly(new VaryingGroup().appendI(0), new CommonGroup().append(1, 0));
    assertThat(newLcs("abab", "aab").getGroups()).containsExactly(new CommonGroup().append(0, 0), new VaryingGroup().appendI(1), new CommonGroup().append(2, 1).append(3, 2));

    Lcs<Character> lcs = newLcs("ab", "a");
    assertThat(lcs.getGroups()).containsExactly(new CommonGroup().append(0, 0), new VaryingGroup().appendI(1));
    assertThat(lcs.getGroups()).containsExactly(new CommonGroup().append(0, 0), new VaryingGroup().appendI(1));
  }

  private Lcs<Character> newLcs(String inputI, String inputJ) {
    return new Lcs<Character>(new TestCharacterElementSequence(inputI), new TestCharacterElementSequence(inputJ), new TestCharacterComparator());
  }

}
