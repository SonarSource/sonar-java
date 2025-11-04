/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class DefaultModuleMetadataTest {

  @Test
  void test() {
    var sonarComponents = mockSonarComponents();
    var config = mockConfiguration();
    sonarComponents.setSensorContext(mockSensorContext(config));
    var defaultModuleMetadata = new DefaultModuleMetadata(sonarComponents, config);

    assertThat(defaultModuleMetadata.moduleKey()).isEqualTo("pmodule/cmodule");
    assertThat(defaultModuleMetadata.javaVersion().asInt()).isEqualTo(-1);
    assertThat(defaultModuleMetadata.shouldIgnoreUnnamedModuleForSplitPackage()).isFalse();
  }

  @Test
  void testWithJavaVersion() {
    var sonarComponents = mockSonarComponents();
    var config = mockConfiguration("sonar.java.source", "11");
    sonarComponents.setSensorContext(mockSensorContext(config));
    var defaultModuleMetadata = new DefaultModuleMetadata(sonarComponents, config);

    assertThat(defaultModuleMetadata.moduleKey()).isEqualTo("pmodule/cmodule");
    assertThat(defaultModuleMetadata.javaVersion().asInt()).isEqualTo(11);
  }

  @Test
  void testWithShouldIgnoreUnnamed() {
    var sonarComponents = mockSonarComponents();
    var config = mockConfiguration("sonar.java.ignoreUnnamedModuleForSplitPackage", "true");
    sonarComponents.setSensorContext(mockSensorContext(config));
    var defaultModuleMetadata = new DefaultModuleMetadata(sonarComponents, config);

    assertThat(defaultModuleMetadata.moduleKey()).isEqualTo("pmodule/cmodule");
    assertThat(defaultModuleMetadata.shouldIgnoreUnnamedModuleForSplitPackage()).isTrue();
  }

  private SonarComponents mockSonarComponents() {
    var rootProj = mock(ProjectDefinition.class);
    doReturn(new File("/foo/bar/proj")).when(rootProj).getBaseDir();
    var childModule = mock(ProjectDefinition.class);
    doReturn(new File("/foo/bar/proj/pmodule/cmodule")).when(childModule).getBaseDir();
    doReturn(rootProj).when(childModule).getParent();

    return new SonarComponents(null, null, null, null, null, null, childModule);
  }

  private Configuration mockConfiguration(String... keysAndValues) {
    Configuration configuration = mock(Configuration.class);
    for (int i = 0; i < keysAndValues.length; i++) {
      String key = keysAndValues[i++];
      String value = keysAndValues[i];
      doReturn(Optional.of(value)).when(configuration).get(key);
      if(value.equals("true") || value.equals("false")) {
        doReturn(Optional.of(Boolean.valueOf(value))).when(configuration).getBoolean(key);
      }
    }
    return configuration;
  }

  private SensorContext mockSensorContext(Configuration config) {
    var sctx = mock(SensorContext.class);
    doReturn(config).when(sctx).config();
    return sctx;
  }

}
