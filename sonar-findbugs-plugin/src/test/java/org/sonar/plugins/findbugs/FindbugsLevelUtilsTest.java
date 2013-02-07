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
package org.sonar.plugins.findbugs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rules.RulePriority;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsLevelUtilsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private FindbugsLevelUtils level = new FindbugsLevelUtils();

  @Test
  public void test() {
    assertThat(level.from("1")).isEqualTo(RulePriority.BLOCKER);
    assertThat(level.from("2")).isEqualTo(RulePriority.MAJOR);
    assertThat(level.from("3")).isEqualTo(RulePriority.INFO);

    assertThat(level.from(RulePriority.BLOCKER)).isEqualTo("1");
    assertThat(level.from(RulePriority.CRITICAL)).isEqualTo("1");
    assertThat(level.from(RulePriority.MAJOR)).isEqualTo("2");
    assertThat(level.from(RulePriority.MINOR)).isEqualTo("2");
    assertThat(level.from(RulePriority.INFO)).isEqualTo("3");
  }

  @Test
  public void unsupported_findbugs_priority() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Priority not supported: 4");
    level.from("4");
  }

}
