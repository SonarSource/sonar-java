/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java.api;

import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;

public class JavaSettings implements BatchExtension, ServerExtension {

  private static final String PROPERTY_COVERAGE_PLUGIN = "sonar.java.coveragePlugin";

  private final Settings settings;

  public JavaSettings(Settings settings) {
    this.settings = settings;
  }

  /**
   * @since 1.1
   */
  public String getEnabledCoveragePlugin() {
    // backward-compatibility with the property that has been deprecated in sonar 3.4.
    String[] keys = settings.getStringArray("sonar.core.codeCoveragePlugin");
    if (keys.length > 0) {
      return keys[0];
    }
    return settings.getString(PROPERTY_COVERAGE_PLUGIN);
  }

  public static PropertyDefinition property() {
    return PropertyDefinition.builder(PROPERTY_COVERAGE_PLUGIN)
      .defaultValue("jacoco")
      .category("java")
      .subCategory("General")
      .name("Code Coverage Plugin")
      .description("Key of the code coverage plugin to use for unit tests.")
      .onQualifiers(Qualifiers.PROJECT)
      .build();
  }

}
