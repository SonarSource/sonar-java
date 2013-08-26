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
package org.sonar.plugins.checkstyle;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public final class CheckstylePlugin extends SonarPlugin {

  private static final String CHECKSTYLE_SUB_CATEGORY_NAME = "Checkstyle";

  @Override
  public List<?> getExtensions() {
    return ImmutableList.of(
        PropertyDefinition.builder(CheckstyleConstants.FILTERS_KEY)
            .defaultValue(CheckstyleConstants.FILTERS_DEFAULT_VALUE)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(CHECKSTYLE_SUB_CATEGORY_NAME)
            .name("Filters")
            .description("Checkstyle support three error filtering mechanisms : SuppressionCommentFilter, SuppressWithNearbyCommentFilter and SuppressionFilter."
              + "This property allows to configure all those filters with a native XML format."
              + " See <a href='http://checkstyle.sourceforge.net/config.html'>Checkstyle configuration page</a> to get more information on those filters.")
            .type(PropertyType.TEXT)
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .build(),
        PropertyDefinition.builder(CheckstyleConfiguration.PROPERTY_GENERATE_XML)
            .defaultValue("false")
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory(CHECKSTYLE_SUB_CATEGORY_NAME)
            .name("Generate XML Report")
            .type(PropertyType.BOOLEAN)
            .hidden()
            .build(),

        CheckstyleSensor.class,
        CheckstyleConfiguration.class,
        CheckstyleExecutor.class,
        CheckstyleAuditListener.class,
        CheckstyleProfileExporter.class,
        CheckstyleProfileImporter.class,
        CheckstyleRuleRepository.class);
  }

}
