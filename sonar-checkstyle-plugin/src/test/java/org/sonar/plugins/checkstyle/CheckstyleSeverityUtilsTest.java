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
package org.sonar.plugins.checkstyle;

import org.junit.Test;
import org.sonar.api.rules.RulePriority;

import static org.fest.assertions.Assertions.assertThat;

public class CheckstyleSeverityUtilsTest {

  @Test
  public void testToSeverity() {
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.BLOCKER)).isEqualTo("error");
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.CRITICAL)).isEqualTo("error");
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.MAJOR)).isEqualTo("warning");
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.MINOR)).isEqualTo("info");
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.INFO)).isEqualTo("info");
  }

  @Test
  public void testFromSeverity() {
    assertThat(CheckstyleSeverityUtils.fromSeverity("error")).isEqualTo(RulePriority.BLOCKER);
    assertThat(CheckstyleSeverityUtils.fromSeverity("warning")).isEqualTo(RulePriority.MAJOR);
    assertThat(CheckstyleSeverityUtils.fromSeverity("info")).isEqualTo(RulePriority.INFO);
    assertThat(CheckstyleSeverityUtils.fromSeverity("")).isNull();
  }

}
