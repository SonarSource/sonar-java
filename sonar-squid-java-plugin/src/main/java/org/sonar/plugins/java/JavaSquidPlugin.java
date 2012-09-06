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
package org.sonar.plugins.java;

import com.google.common.collect.ImmutableList;
import org.sonar.api.*;
import org.sonar.plugins.java.decorators.*;

import java.util.List;

@Properties({
  @Property(
    key = JavaSquidPlugin.SQUID_ANALYSE_ACCESSORS_PROPERTY,
    defaultValue = JavaSquidPlugin.SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE + "",
    name = "Separate accessors",
    description = "Flag whether Squid should separate accessors (getters/setters) from methods. " +
        "In that case, accessors are not counted in metrics such as complexity or API documentation.",
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_JAVA,
    type = PropertyType.BOOLEAN),
  @Property(
    key = JavaSquidPlugin.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION,
    defaultValue = JavaSquidPlugin.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION_DEFAULT_VALUE,
    name = "List of fields to exclude from LCOM4 computation",
    description = "Some fields should not be taken into account when computing LCOM4 measure as they " +
        "unexpectedly and artificially decrease the LCOM4 measure. " +
        "The best example is a logger used by all methods of a class. " +
        "All field names to exclude from LCOM4 computation must be separated by a comma.",
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_JAVA),
  @Property(
    key = CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY,
    defaultValue = "" + CoreProperties.DESIGN_SKIP_DESIGN_DEFAULT_VALUE,
    name = "Skip design analysis",
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_JAVA,
    type = PropertyType.BOOLEAN)
})
public class JavaSquidPlugin extends SonarPlugin {

  public static final String SQUID_ANALYSE_ACCESSORS_PROPERTY = "sonar.squid.analyse.property.accessors";
  public static final boolean SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE = true;

  public static final String FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION = "sonar.squid.fieldsToExcludeFromLcom4Computation";
  public static final String FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION_DEFAULT_VALUE = "LOG, logger";

  public List<?> getExtensions() {
    return ImmutableList.of(
        JavaSourceImporter.class,
        JavaRuleRepository.class,
        JavaSonarWayProfile.class,
        JavaSonarWayWithFindbugsProfile.class,
        JavaSquidSensor.class,
        ChidamberKemererDistributionBuilder.class,
        ClassesDecorator.class,
        FileComplexityDistributionDecorator.class,
        FunctionComplexityDistributionBuilder.class,
        FunctionsDecorator.class);
  }

}
