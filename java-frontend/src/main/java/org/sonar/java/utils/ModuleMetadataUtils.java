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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

public class ModuleMetadataUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ModuleMetadataUtils.class);

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
    while (current != null && current.getParent() != null) {
      current = current.getParent();
    }
    return current;
  }

  public static String getFullyQualifiedModuleKey(@Nullable ProjectDefinition current) {
    if (current == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    // we do not want to include root module as this is usually either a sonar project key (as in Gradle) or a (Maven) groupId
    while (current != null && current.getParent() != null) {
      // prepend separator if not first module
      if (!builder.isEmpty()) {
        // as modules can have dots in names, separator should be :
        builder.insert(0, ":");
      }
      // get module key property
      var property = current.properties().get("sonar.moduleKey");
      if (property != null) {
        LOG.warn("         sonar.moduleKey={}", property);
        var leafModule = property.lastIndexOf(":") >= 0
          ? property.substring(property.lastIndexOf(":") + 1)
          : property;
        builder.insert(0, leafModule);
        current = current.getParent();
      } else {
        break;
      }
    }
    LOG.warn("getFullyQualifiedModuleKey={}", builder);
    return builder.toString();
  }

  public static void printProjectDefinitionHierarchyAndProperties(ProjectDefinition current, String indent) {
    current.properties().forEach((key, value) -> {
      if (key.contains("moduleKey")) {
        LOG.warn("{}Project: {}, Property: {}={}", indent, current.getName(), key, value);
      }
    });
    for (var child : current.getSubProjects()) {
      printProjectDefinitionHierarchyAndProperties(child, indent + "  ");
    }
  }
}
