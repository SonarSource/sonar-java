/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.io.File;
import java.util.List;
import org.sonar.api.config.Configuration;
import org.sonar.plugins.java.api.JavaVersion;

/**
 * A frontend that can stand in for the default one. Implementations are discovered with {@link java.util.ServiceLoader}
 * and consulted every time a {@link JParser} is created, so the frontend can change between analyses without any
 * caller knowing about it.
 */
public interface JParserProvider {

  /** Called once at the start of each analysis, before any parser is created. */
  void configure(Configuration configuration);

  /** Whether the parsers of this provider must be used for the analysis in progress. */
  boolean isEnabled();

  JParser create(JParser.Mode mode, JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage);
}
