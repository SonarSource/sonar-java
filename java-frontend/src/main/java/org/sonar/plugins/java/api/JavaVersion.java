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
   * Flag to enable java preview features
   */
  String ENABLE_PREVIEW = "sonar.java.enablePreview";

  /**
   * Test if java version of the project is not set or greater than or equal to 6.
   * @return true if java version used is >= 6 or not set
   */
  boolean isJava6Compatible();

  /**
   * Test if java version of the project is not set or greater than or equal to 7.
   * @return true if java version used is >= 7 or not set
   */
  boolean isJava7Compatible();

  /**
   * Test if java version of the project is not set greater than or equal to 8.
   * @return true if java version used is >= 8 or not set
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
   * Test if java version of the project is greater than or equal to 17.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 17
   * @since SonarJava 7.14: Support of Java 18
   */
  boolean isJava17Compatible();

  /**
   * Test if java version of the project is greater than or equal to 18.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 18
   * @since SonarJava 7.14: Support of Java 18
   */
  boolean isJava18Compatible();

  /**
   * Test if java version of the project is greater than or equal to 19.
   * Remark - Contrary to other isJava*Compatible methods, this one will NOT return true if version is not set
   * @return true if java version used is >= 19
   * @since SonarJava 7.19: Support of Java 19
   */
  boolean isJava19Compatible();

  /**
   * get java version as integer
   * @return an int representing the java version
   */
  int asInt();

  /**
   * Test if java version has been set for the analysis.
   * @return true if set, false otherwise.
   */
  boolean isSet();

  /**
   * Test if java version has not been set for the analysis.
   * @return true if not set, false otherwise.
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

  /**
   * Get the effective Java version as a String. If no version is set, return the maximum supported version.
   * @return an int representing the effective java version
   */
  String effectiveJavaVersionAsString();
  
  /**
   * Returns wether preview features are enabled or not (false by default)
   * @return true if enabled, false otherwise
   */
  boolean arePreviewFeaturesEnabled(); 
}
