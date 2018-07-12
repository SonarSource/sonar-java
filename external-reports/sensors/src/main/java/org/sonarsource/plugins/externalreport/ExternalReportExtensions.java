/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonarsource.plugins.externalreport;

import org.sonar.api.Plugin.Context;
import org.sonar.api.utils.Version;
import org.sonarsource.plugins.externalreport.checkstyle.CheckstyleSensor;

public class ExternalReportExtensions {

  public static final String EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers";

  public static final String JAVA_SUBCATEGORY = "Java";

  public static void define(Context context) {
    boolean externalIssuesSupported = context.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(7, 2));
    CheckstyleSensor.define(context, externalIssuesSupported);
  }

}
