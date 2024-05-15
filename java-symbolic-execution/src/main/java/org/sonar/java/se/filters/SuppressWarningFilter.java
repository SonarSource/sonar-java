/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.se.filters;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.analyzer.commons.collections.MapBuilder;

public class SuppressWarningFilter extends BaseTreeVisitorSEIssueFilter {

  //S2259 NullDereferenceCheck
  //S2095 UnclosedResourcesCheck
  //S2583 ConditionalUnreachableCodeCheck
  //S3518 DivisionByZeroCheck


  private static final Map<String, Set<String>> JAVAC_WARNING_SUPPRESSING_RULES = MapBuilder.<String, Set<String>>newMap()
    // JDK warnings
    .put("divzero", Collections.singleton("java:S3518"))
    // Eclipse (IDE) warnings
    .put("null", Collections.singleton("java:S2259"))
    .put("resource", Collections.singleton("java:S2095"))
    .put("unused", Collections.singleton("java:S2583"))
    .build();

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return Set.of();
  }

}
