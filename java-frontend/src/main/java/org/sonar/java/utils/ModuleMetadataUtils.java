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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

public class ModuleMetadataUtils {

  private ModuleMetadataUtils() {
    // utility class
  }

  public static String getModuleKey(@Nullable ProjectDefinition projectDefinition) {
    var root = getRootProject(projectDefinition);
    if (root != null && projectDefinition != null) {
      var rootBase = root.getBaseDir().toPath();
      var moduleBase = projectDefinition.getBaseDir().toPath();
      return rootBase.relativize(moduleBase).toString().replace('\\', '/');
    }
    return "";
  }

  @CheckForNull
  public static ProjectDefinition getRootProject(@Nullable ProjectDefinition projectDefinition) {
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
