/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.plugins.java.api;

import com.google.common.annotations.Beta;

/**
 * Represents the java version used by the project under analysis.
 * Destinated to be used by checks to determine if they should report issue depending on java version.
 */
@Beta
public interface JavaVersion {

  /**
   * Test if java version of the project is greater than or equal to 7.
   * @return true if java version used is >= 7
   */
  boolean isJava7Compatible();

  /**
   * Test if java version of the project is greater than or equal to 8.
   * @return true if java version used is >= 8
   */
  boolean isJava8Compatible();

  /**
   * get java version as integer
   * @return an int representing the java version
   */
  int asInt();

  /**
   * Test if java version has been set for the analysis.
   * @return false if set, true otherwise.
   */
  boolean isNotSet();

  /**
   * Default compatibility message with java 7
   * @return empty string if java version is properly set, default message otherwise.
   */
  String java7CompatibilityMessage();

  /**
   * Default compatibility message with java 8
   * @return empty string if java version is properly set, default message otherwise.
   */
  String java8CompatibilityMessage();
}
