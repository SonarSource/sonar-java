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
package org.sonar.java;

import java.io.File;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StrutsTest extends MeasurerTester {

  private static final File PROJECT_DIR = new File("target/test-projects/struts-core-1.3.9");
  private static final File SOURCE_DIR = new File(PROJECT_DIR, "src");

  @Override
  public File projectDir() {
    return PROJECT_DIR;
  }

  @Override
  public File sourceDir() {
    return SOURCE_DIR;
  }

  @Test
  public void measures_on_project() throws Exception {
    Map<String, Double> metrics = getMetrics();

    assertThat(metrics.get("classes").intValue()).isEqualTo(146);
    assertThat(metrics.get("ncloc").intValue()).isEqualTo(14007);
    assertThat(metrics.get("statements").intValue()).isEqualTo(6403 /* empty statements between members of class */+ 3);
    assertThat(metrics.get("comment_lines").intValue()).isEqualTo(7605);
    assertThat(metrics.get("functions").intValue()).isEqualTo(1429);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(3055);
  }

}
