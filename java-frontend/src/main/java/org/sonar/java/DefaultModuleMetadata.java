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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Configuration;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.utils.ModuleMetadataUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.internal.ModuleMetadata;

import static org.sonar.java.SonarComponents.SONAR_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE;

public class DefaultModuleMetadata implements ModuleMetadata {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultModuleMetadata.class);

  private final JavaVersion javaVersion;
  private final ProjectDefinition projectDefinition;
  private final boolean ignoreUnnamedModuleForSplitPackage;

  public DefaultModuleMetadata(ProjectDefinition projectDefinition, Configuration configuration) {
    this.javaVersion = JavaVersionImpl.readFromConfiguration(configuration);
    this.projectDefinition = projectDefinition;
    this.ignoreUnnamedModuleForSplitPackage = configuration.getBoolean(SONAR_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE).orElse(false);
  }

  @Override
  public JavaVersion javaVersion() {
    return javaVersion;
  }

  @Override
  public String moduleKey() {
    var moduleKey = ModuleMetadataUtils.getModuleKey(projectDefinition);
    if (moduleKey.isEmpty()) {
      LOG.warn("Unable to determine module key, using empty string as fallback");
    }
    return moduleKey;
  }

  @Override
  public boolean shouldIgnoreUnnamedModuleForSplitPackage() {
    return ignoreUnnamedModuleForSplitPackage;
  }

}
