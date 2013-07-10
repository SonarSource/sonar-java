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
package org.sonar.plugins.findbugs;

public final class FindbugsConstants {

  public static final String REPOSITORY_NAME = "Findbugs";
  public static final String PLUGIN_KEY = "findbugs";
  public static final String PLUGIN_NAME = "Findbugs";
  public static final String REPOSITORY_KEY = PLUGIN_KEY;

  public static final String EFFORT_PROPERTY = "sonar.findbugs.effort";
  public static final String EFFORT_DEFAULT_VALUE = "Default";
  public static final String CONFIDENCE_LEVEL_PROPERTY = "sonar.findbugs.confidenceLevel";
  public static final String CONFIDENCE_LEVEL_DEFAULT_VALUE = "medium";
  public static final String TIMEOUT_PROPERTY = "sonar.findbugs.timeout";
  public static final long TIMEOUT_DEFAULT_VALUE = 600000L;

  /**
   * @since 2.10
   */
  public static final String EXCLUDES_FILTERS_PROPERTY = "sonar.findbugs.excludesFilters";

  private FindbugsConstants() {
  }
}
