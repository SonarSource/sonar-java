/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java.api.internal;

import org.sonar.api.batch.ScannerSide;
import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.JavaVersion;


/**
 * Interface to access metadata about the module being analyzed by a Sensor.
 * For internal use only, this API will not be supported for custom plugins.
 */
@Beta
@ScannerSide
public interface ModuleMetadata {

  /**
   * Returns the Java version of the module being analyzed.
   */
  JavaVersion javaVersion();

  /**
   * Returns the module key of the module being analyzed.
   */
  String moduleKey();

  /**
   * Describes whether input files should be parsed while ignoring unnamed split modules.
   */
  boolean shouldIgnoreUnnamedModuleForSplitPackage();

}

