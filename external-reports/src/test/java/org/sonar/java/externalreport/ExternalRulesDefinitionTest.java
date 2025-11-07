/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.externalreport;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalRulesDefinitionTest {

  private static final Path PROJECT_DIR = Paths.get("src", "test", "resources", "spotbugs")
    .toAbsolutePath().normalize();

  @Test
  void toString_should_exist_and_contains_linter_name() {
    // to string is used by compute engine logs and should return a unique key
    SensorContextTester sensorContext = SensorContextTester.create(PROJECT_DIR);
    var spotBugsSensor = new SpotBugsSensor(sensorContext.runtime());
    assertThat(new ExternalRulesDefinition(spotBugsSensor.ruleLoader(), "someLinterKey")).hasToString("someLinterKey-rules-definition");
  }
}
