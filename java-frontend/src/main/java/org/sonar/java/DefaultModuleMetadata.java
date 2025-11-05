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

import org.sonar.api.config.Configuration;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.internal.ModuleMetadata;

public class DefaultModuleMetadata implements ModuleMetadata {

  private final JavaVersion javaVersion;
  private final String moduleKey;

  public DefaultModuleMetadata(SonarComponents sonarComponents, Configuration configuration) {
    this.javaVersion = JavaVersionImpl.readFromConfiguration(configuration);
    this.moduleKey = sonarComponents.getModuleKey();
  }

  @Override
  public JavaVersion javaVersion() {
    return javaVersion;
  }

  @Override
  public String moduleKey() {
    return moduleKey;
  }

}
