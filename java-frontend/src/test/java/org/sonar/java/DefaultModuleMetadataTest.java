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
package org.sonar.java;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.java.TestUtils.mockProjectDefinition;

class DefaultModuleMetadataTest {

  @Test
  void test() {
    var projectDefinition = mockProjectDefinition();
    var config = mockConfiguration();
    var defaultModuleMetadata = new DefaultModuleMetadata(projectDefinition, config);

    assertThat(defaultModuleMetadata.moduleKey()).isEqualTo("pmodule/cmodule");
    assertThat(defaultModuleMetadata.javaVersion().asInt()).isEqualTo(-1);
    assertThat(defaultModuleMetadata.shouldIgnoreUnnamedModuleForSplitPackage()).isFalse();
  }

  @Test
  void testNullProjectDefinition() {
    var config = mockConfiguration();
    var defaultModuleMetadata = new DefaultModuleMetadata(null, config);

    assertThat(defaultModuleMetadata.moduleKey()).isEmpty();
  }

  @Test
  void testWithJavaVersion() {
    var projectDefinition = mockProjectDefinition();
    var config = mockConfiguration("sonar.java.source", "11");
    var defaultModuleMetadata = new DefaultModuleMetadata(projectDefinition, config);

    assertThat(defaultModuleMetadata.moduleKey()).isEqualTo("pmodule/cmodule");
    assertThat(defaultModuleMetadata.javaVersion().asInt()).isEqualTo(11);
  }

  @Test
  void testWithShouldIgnoreUnnamed() {
    var projectDefinition = mockProjectDefinition();
    var config = mockConfiguration("sonar.java.ignoreUnnamedModuleForSplitPackage", "true");
    var defaultModuleMetadata = new DefaultModuleMetadata(projectDefinition, config);

    assertThat(defaultModuleMetadata.moduleKey()).isEqualTo("pmodule/cmodule");
    assertThat(defaultModuleMetadata.shouldIgnoreUnnamedModuleForSplitPackage()).isTrue();
  }

  private Configuration mockConfiguration(String... keysAndValues) {
    Configuration configuration = mock(Configuration.class);
    for (int i = 0; i < keysAndValues.length; i++) {
      String key = keysAndValues[i++];
      String value = keysAndValues[i];
      doReturn(Optional.of(value)).when(configuration).get(key);
      if (value.equals("true") || value.equals("false")) {
        doReturn(Optional.of(Boolean.valueOf(value))).when(configuration).getBoolean(key);
      }
    }
    return configuration;
  }

}
