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
package org.sonar.java.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.sonar.java.TestUtils.mockProjectDefinition;
import static org.sonar.java.TestUtils.mockProjectDefinitionWithModuleKeys;

class ModuleMetadataUtilsTest {

  @Test
  void getModuleKey() {
    var projectDefinition = mockProjectDefinition();
    assertThat(ModuleMetadataUtils.getModuleKey(projectDefinition)).isEqualTo("pmodule/cmodule");
    assertThat(ModuleMetadataUtils.getModuleKey(null)).isEmpty();
  }

  @Test
  void getFullyQualifiedModuleKey() {
    var projectDefinition = mockProjectDefinitionWithModuleKeys();
    assertThat(ModuleMetadataUtils.getFullyQualifiedModuleKey(projectDefinition)).isEqualTo("module1:module2");
    assertThat(ModuleMetadataUtils.getFullyQualifiedModuleKey(null)).isEmpty();
  }

  @Test
  void getRootProject() {
    var projectDefinition = mockProjectDefinition();
    assertSame(ModuleMetadataUtils.getRootProject(projectDefinition), ModuleMetadataUtils.getRootProject(projectDefinition.getParent()));
    assertNull(ModuleMetadataUtils.getRootProject(null));
  }
}
