/*
 * Sonar Java
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
package org.sonar.plugins.cobertura;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.cobertura.base.CoberturaConstants;

import java.util.List;

public final class CoberturaPlugin extends SonarPlugin {

  static final String PLUGIN_KEY = "cobertura";

  private static final String COBERTURA_SUBCATEGORY_NAME = "Cobertura";

  public List<?> getExtensions() {
    return ImmutableList.of(
      PropertyDefinition.builder(CoberturaConstants.COBERTURA_REPORT_PATH_PROPERTY)
        .category(CoreProperties.CATEGORY_JAVA)
        .subCategory(COBERTURA_SUBCATEGORY_NAME)
        .name("Report path")
        .description("Path (absolute or relative) to Cobertura xml report file.")
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition.builder(CoberturaConstants.COBERTURA_MAXMEM_PROPERTY)
        .defaultValue(CoberturaConstants.COBERTURA_MAXMEM_DEFAULT_VALUE)
        .category(CoreProperties.CATEGORY_JAVA)
        .subCategory(COBERTURA_SUBCATEGORY_NAME)
        .name("Maxmem")
        .description("Maximum memory to pass to JVM of Cobertura processes.")
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      CoberturaSettings.class,
      CoberturaSensor.class,
      CoberturaMavenPluginHandler.class,
      CoberturaMavenInitializer.class);
  }

}
