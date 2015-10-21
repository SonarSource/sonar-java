/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.checks.helpers;

import javax.annotation.Nullable;

public class JavaVersionHelper {

  private static final int JAVA_7 = 7;
  private static final int JAVA_8 = 8;

  private JavaVersionHelper() {
  }

  public static boolean java7Compatible(@Nullable Integer javaVersion) {
    return notSetOrAtLeast(javaVersion, JAVA_7);
  }

  public static boolean java8Compatible(@Nullable Integer javaVersion) {
    return notSetOrAtLeast(javaVersion, JAVA_8);
  }

  private static boolean notSetOrAtLeast(@Nullable Integer providedJavaVersion, Integer requiredJavaVersion) {
    return providedJavaVersion == null || isAtLeast(providedJavaVersion, requiredJavaVersion);
  }

  private static boolean isAtLeast(Integer provided, Integer required) {
    return required <= provided;
  }
}
