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
package org.sonar.java;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.plugins.java.api.internal.ClasspathResolver;
import org.sonar.plugins.java.api.internal.ResolvedClasspath;

public class DefaultClassPathResolver implements ClasspathResolver {
  private SonarComponents sonarComponents;

  public DefaultClassPathResolver(SonarComponents sonarComponents) {
    this.sonarComponents = sonarComponents;
  }

  @Override
  public ResolvedClasspath mainClasspath() {
    return new ResolvedClasspathImpl(SonarComponentsAdapter.forMainSources(sonarComponents));
  }

  @Override
  public ResolvedClasspath testClasspath() {
    return new ResolvedClasspathImpl(SonarComponentsAdapter.forTestSources(sonarComponents));
  }

  private interface SonarComponentsAdapter {
    List<File> binaries();

    List<File> classpath();

    static SonarComponentsAdapter forMainSources(SonarComponents sonarComponents) {
      return new SonarComponentsAdapter() {
        @Override
        public List<File> binaries() {
          return sonarComponents.getCompilationOutput();
        }

        @Override
        public List<File> classpath() {
          return sonarComponents.getJavaClasspath();
        }
      };
    }

    static SonarComponentsAdapter forTestSources(SonarComponents sonarComponents) {
      return new SonarComponentsAdapter() {
        @Override
        public List<File> binaries() {
          return sonarComponents.getTestCompilationOutput();
        }

        @Override
        public List<File> classpath() {
          return sonarComponents.getJavaTestClasspath();
        }
      };
    }
  }

  static class ResolvedClasspathImpl implements ResolvedClasspath {
    private final List<Path> binaries;
    private final List<Path> libraries;

    ResolvedClasspathImpl(SonarComponentsAdapter sonarComponentsAdapter) {
      binaries = toPaths(sonarComponentsAdapter.binaries()).toList();
      var binariesSet = new HashSet<>(binaries);

      libraries = toPaths(sonarComponentsAdapter.classpath())
        .filter(classpathEntry -> !binariesSet.contains(classpathEntry))
        .toList();
    }

    @Override
    public List<Path> sonarJavaBinaries() {
      return binaries;
    }

    @Override
    public List<Path> sonarJavaLibraries() {
      return libraries;
    }

    private static Stream<Path> toPaths(List<File> files) {
      return files.stream().map(File::toPath);
    }
  }
}
