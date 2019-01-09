/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.java.matcher;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NameCriteriaTest {

  @Test
  public void should_match_any() {
    NameCriteria nc = NameCriteria.any();
    assertThat(nc.test(null)).isTrue();
    assertThat(nc.test("equal")).isTrue();
  }

  @Test
  public void should_match_exact_name() {
    NameCriteria nc = NameCriteria.is("equal");
    assertThat(nc.test("foo")).isFalse();
    assertThat(nc.test("equal")).isTrue();
  }

  @Test
  public void should_match_prefix() {
    NameCriteria nc = NameCriteria.startsWith("get");
    assertThat(nc.test("equal")).isFalse();
    assertThat(nc.test("get")).isTrue();
    assertThat(nc.test("getObject")).isTrue();
  }
}
