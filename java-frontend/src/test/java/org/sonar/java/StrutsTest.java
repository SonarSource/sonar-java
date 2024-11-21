/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrutsTest extends MeasurerTester {

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
  void measures_on_project() {
    Map<String, Double> metrics = getMetrics();

    assertThat(metrics.get("classes").intValue()).isEqualTo(146);
    assertThat(metrics.get("ncloc").intValue()).isEqualTo(14007);
    assertThat(metrics.get("statements").intValue()).isEqualTo(6403 /* empty statements between members of class */+ 3);
    assertThat(metrics.get("comment_lines").intValue()).isEqualTo(7605);
    assertThat(metrics.get("functions").intValue()).isEqualTo(1429);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(3055);
  }

}
