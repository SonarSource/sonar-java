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
package org.sonar.java.classpath;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.plugins.java.api.classpath.DependencyVersion;

public interface DependencyVersionInference {

  Optional<DependencyVersion> infer(List<File> classpath);

  String getGroupId();

  String getArtifactId();

  abstract class DependencyVersionInferenceBase implements DependencyVersionInference {
    final String groupId;
    final String artifactId;

    protected DependencyVersionInferenceBase(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    @Override
    public String getGroupId() {
      return groupId;
    }

    @Override
    public String getArtifactId() {
      return artifactId;
    }
  }


  class ByNameInference extends DependencyVersionInferenceBase {

    final Pattern pattern;

    protected ByNameInference(Pattern pattern, String groupId, String artifactId) {
      super(groupId, artifactId);
      this.pattern = pattern;
    }

    @Override
    public Optional<DependencyVersion> infer(List<File> classpath) {
      for (File file : classpath) {
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.matches()) {
          return Optional.of(new DependencyVersionImpl(
            groupId, artifactId, Version.matcherToVersion(matcher)));
        }
      }
      return Optional.empty();
    }
  }

  class ReflectiveInference extends DependencyVersionInferenceBase {
    private static final String KNOWN_CLASS_NAME = "lombok.Lombok";

    // TODO generalize for other libraries
    protected ReflectiveInference() {
      super("org.projectlombok", "lombok");
    }

    @Override
    public Optional<DependencyVersion> infer(List<File> classpath) {
      URLClassLoader loader = new URLClassLoader(classpath.stream().map(file -> {
        try {
          return file.toURL();
        } catch (MalformedURLException e) {
          return null;
        }
      }).filter(Objects::nonNull).toArray(URL[]::new), null);
      try {
        Class<?> knownClass = loader.loadClass(KNOWN_CLASS_NAME);
        String implementationVersion = knownClass.getPackage().getImplementationVersion();
        return Version.parse(implementationVersion).map(v ->
          new DependencyVersionImpl(groupId, artifactId, v));
      } catch (ClassNotFoundException e) {
        return Optional.empty();
      }
    }
  }

  class ManifestInference extends DependencyVersionInferenceBase {

    final String attributeName;

    public ManifestInference(String attributeName, String groupId, String artifactId) {
      super(groupId, artifactId);
      this.attributeName = attributeName;
    }

    @Override
    public Optional<DependencyVersion> infer(List<File> classpath) {
      Optional<File> lombokJar = classpath.stream().filter(file -> file.getName().startsWith(artifactId)).findFirst();
      if (lombokJar.isEmpty()) return Optional.empty();

      try {
        URL jarUrl = new URL("jar:file:" + lombokJar.get().getAbsolutePath() + "!/META-INF/MANIFEST.MF");
        JarURLConnection jarConnection = (JarURLConnection) jarUrl.openConnection();
        Manifest manifest = jarConnection.getManifest();

        if (manifest != null) {
          Attributes mainAttributes = manifest.getMainAttributes();
          if (mainAttributes != null) {
            Optional<Version> version =
              Version.parse(mainAttributes.getValue(attributeName));
            if (version.isPresent()) {
              return Optional.of(
                new DependencyVersionImpl(groupId, artifactId, version.get()));
            }
          }
        }
      } catch (IOException ignored) {
      }
      return Optional.empty();
    }
  }

  class FallbackInference implements DependencyVersionInference {

    final DependencyVersionInference mainInference;
    final DependencyVersionInference fallback;

    public FallbackInference(DependencyVersionInference mainInference, DependencyVersionInference fallback) {
      this.mainInference = mainInference;
      this.fallback = fallback;
    }

    public FallbackInference make(DependencyVersionInference mainInference, DependencyVersionInference fallback) {
      if (!(mainInference.getGroupId().equals(fallback.getGroupId()) &&
        mainInference.getArtifactId().equals(fallback.getArtifactId()))) {
        throw new IllegalArgumentException();
      }
      return new FallbackInference(mainInference, fallback);
    }

    @Override
    public Optional<DependencyVersion> infer(List<File> classpath) {
      Optional<DependencyVersion> inferred = mainInference.infer(classpath);
      if (inferred.isPresent()) {
        return inferred;
      }
      return fallback.infer(classpath);
    }

    @Override
    public String getGroupId() {
      return mainInference.getGroupId();
    }

    @Override
    public String getArtifactId() {
      return mainInference.getArtifactId();
    }
  }
}
