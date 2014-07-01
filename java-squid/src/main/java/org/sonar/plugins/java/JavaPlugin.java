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
package org.sonar.plugins.java;

import com.google.common.collect.ImmutableList;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public final class JavaPlugin {

  public static List getExtensions() {
    return ImmutableList.of(
        JavaCommonRulesEngine.class,
        JavaCommonRulesDecorator.class,
        Java.class,
        CommonRulesSonarWayProfile.class,
        CommonRulesSonarWayWithFindbugsProfile.class,
        PropertyDefinition.builder(Java.FILE_SUFFIXES_KEY)
            .defaultValue(Java.DEFAULT_FILE_SUFFIXES)
            .name("File suffixes")
            .description("Comma-separated list of suffixes for files to analyze. To not filter, leave the list empty.")
            .subCategory("General")
            .onQualifiers(Qualifiers.PROJECT)
            .build()
    );
  }

}
