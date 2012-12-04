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
package org.sonar.plugins.findbugs;

import org.sonar.api.CoreProperties;

public final class FindbugsConstants {

  public static final String REPOSITORY_KEY = CoreProperties.FINDBUGS_PLUGIN;
  public static final String REPOSITORY_NAME = "Findbugs";
  public static final String PLUGIN_NAME = "Findbugs";
  public static final String PLUGIN_KEY = CoreProperties.FINDBUGS_PLUGIN;

  /**
   * @since 2.10
   */
  public static final String EXCLUDES_FILTERS_PROPERTY = "sonar.findbugs.excludesFilters";

  private FindbugsConstants() {
  }
}
