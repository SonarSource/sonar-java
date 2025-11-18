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

import javax.annotation.CheckForNull;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Configuration;
import org.sonar.java.classpath.ClasspathProperties;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.internal.ModuleMetadata;

import static org.sonar.java.SonarComponents.SONAR_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE;

public class DefaultModuleMetadata implements ModuleMetadata {

  private final JavaVersion javaVersion;
  private final ProjectDefinition projectDefinition;
  private final boolean ignoreUnnamedModuleForSplitPackage;
  private final String jdkHome;

  public DefaultModuleMetadata(ProjectDefinition projectDefinition, Configuration configuration) {
    this.javaVersion = JavaVersionImpl.readFromConfiguration(configuration);
    this.projectDefinition = projectDefinition;
    this.ignoreUnnamedModuleForSplitPackage = configuration.getBoolean(SONAR_IGNORE_UNNAMED_MODULE_FOR_SPLIT_PACKAGE).orElse(false);
    this.jdkHome = configuration.get(ClasspathProperties.SONAR_JAVA_JDK_HOME).orElse("");
  }

  @Override
  public JavaVersion javaVersion() {
    return javaVersion;
  }

  @Override
  public String jdkHome() {
    return jdkHome;
  }

  @Override
  public String moduleKey() {
    var root = getRootProject();
    if (root != null && projectDefinition != null) {
      var rootBase = root.getBaseDir().toPath();
      var moduleBase = projectDefinition.getBaseDir().toPath();
      return rootBase.relativize(moduleBase).toString().replace('\\', '/');
    }
    return "";
  }

  @Override
  public boolean shouldIgnoreUnnamedModuleForSplitPackage() {
    return ignoreUnnamedModuleForSplitPackage;
  }

  @CheckForNull
  private ProjectDefinition getRootProject() {
    ProjectDefinition current = projectDefinition;
    if (current == null) {
      return null;
    }
    while (current.getParent() != null) {
      current = current.getParent();
    }
    return current;
  }

}
