/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

class CommonsCollectionsTest extends MeasurerTester {

  private static final File PROJECT_DIR = new File("target/test-projects/commons-collections-3.2.1");
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

    assertThat(metrics.get("classes").intValue()).isEqualTo(412);
    assertThat(metrics.get("ncloc").intValue()).isEqualTo(26323);
    assertThat(metrics.get("statements").intValue()).isEqualTo(12047);
    assertThat(metrics.get("comment_lines").intValue()).isEqualTo(17908);
    assertThat(metrics.get("functions").intValue()).isEqualTo(3762);
    assertThat(metrics.get("complexity").intValue()).isEqualTo(6714);
  }

}
