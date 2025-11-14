/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.plugins.java.api.Version;

public class DependencyVersionInference {

  /** Cache for dependency retrieval. Indexed by artifactId. */
  private final Map<String, Optional<Version>> dependencyVersionsCache = new HashMap<>();

  static Pattern makeJarPattern(String artifactId) {
    return Pattern.compile(artifactId + "-" + VersionImpl.VERSION_REGEX + "\\.jar");
  }

  public Optional<Version> infer(String artifactId, List<File> classpath) {
    return dependencyVersionsCache
      .computeIfAbsent(artifactId, key -> infer(makeJarPattern(key), classpath));
  }

  private static Optional<Version> infer(Pattern jarPattern, List<File> classpath) {
    for (File file : classpath) {
      Matcher matcher = jarPattern.matcher(file.getName());
      if (matcher.matches()) {
        return Optional.of(VersionImpl.matcherToVersion(matcher));
      }
    }
    return Optional.empty();
  }
}
