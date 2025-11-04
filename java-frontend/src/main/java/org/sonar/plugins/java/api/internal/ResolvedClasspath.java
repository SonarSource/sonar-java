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

import java.nio.file.Path;
import java.util.List;

/**
 * THIS API IS ONLY BE CONSUMED BY PLUGINS MAINTAINED BY SONARSOURCE.
 * IT IS NOT STABLE AND MAY CHANGE AT ANY TIME.
 * <p>
 * Resolved classpath information (compilation outputs and libraries) for either the main or test sources of one project
 * (i.e. module in Maven terms).
 * Instances of this can be obtained from {@link ClasspathResolver}.
 */
public interface ResolvedClasspath {
  List<Path> sonarJavaBinaries();

  List<Path> sonarJavaLibraries();
}
