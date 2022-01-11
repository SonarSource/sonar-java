/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.sonar.java.annotations.Beta;

/**
 * Represents the java version used by the project under analysis.
 * Designed to be used by checks to determine if they should report issue depending on java version.
 */
@Beta
public interface JavaVersion {

  /**
   * Key of the java version used for sources
   */
  String SOURCE_VERSION = "sonar.java.source";

  /**
   * Test if java version of the project is greater than or equal to 6.
   * @return true if java version used is >= 6
   */
  boolean isJava6Compatible();

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
   * Test if java version of the project is greater than or equal to 9.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 9
   * @since SonarJava 6.15
   */
  boolean isJava9Compatible();

  /**
   * Test if java version of the project is greater than or equal to 10.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 10
   * @since SonarJava 6.15
   */
  boolean isJava10Compatible();

  /**
   * Test if java version of the project is greater than or equal to 12.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 12
   * @since SonarJava 5.12: Support of Java 12
   */
  boolean isJava12Compatible();

  /**
   * Test if java version of the project is greater than or equal to 14.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 14
   * @since SonarJava 6.15
   */
  boolean isJava14Compatible();

  /**
   * Test if java version of the project is greater than or equal to 15.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 15
   * @since SonarJava 6.12: Support of Java 15
   */
  boolean isJava15Compatible();

  /**
   * Test if java version of the project is greater than or equal to 16.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 16
   * @since SonarJava 7.1: Support of Java 16
   */
  boolean isJava16Compatible();

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
   * Default compatibility message with java 6
   * @return empty string if java version is properly set, default message otherwise.
   */
  String java6CompatibilityMessage();

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
