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

import org.sonar.api.scanner.ScannerSide;

/**
 * THIS API IS ONLY BE CONSUMED BY PLUGINS MAINTAINED BY SONARSOURCE.
 * IT IS NOT STABLE AND MAY CHANGE AT ANY TIME.
 * <p>
 * Provides classpath information (compilation outputs and libraries) for main and test sources.
 * Essentially, these are the paths that result from SonarJava's internal resolution of the {@code sonar.java[.test].binaries} and
 * {@code sonar.java[.test].libraries} properties.
 * <p>
 * This is an extension that is instantiated per-project (per-module in Maven terms) and can be injected when needed.
 */
@ScannerSide
public interface ClasspathResolver {
  ResolvedClasspath mainClasspath();

  ResolvedClasspath testClasspath();
}
