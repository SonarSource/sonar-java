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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.classpath.DependencyVersion;

public interface DependencyVersionInference {

  Optional<DependencyVersion> infer(List<File> classpath);

  boolean handles(String groupId, String artifactId);

  String VERSION_REGEX = "([0-9]+).([0-9]+).([0-9]+)([^0-9].*)?";
  Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEX);

  @VisibleForTesting
  Pattern LOMBOK_PATTERN = Pattern.compile("lombok-([0-9]+).([0-9]+).([0-9]+)([^0-9].*)?\\.jar");

  List<DependencyVersionInference> inferenceImplementations = Arrays.asList(
    new ByNameInference(LOMBOK_PATTERN, "org.projectlombok", "lombok"),
    new ManifestInference("Lombok-Version", "org.projectlombok", "lombok"),
    new ManifestInference("Implementation-Version", "org.springframework.boot", "spring-boot"),
    new ByNameInference(Pattern.compile("spring-boot-" + VERSION_REGEX + "\\.jar"),
      "org.springframework.boot", "spring-boot")
  );

  static Version matcherToVersion(Matcher matcher) {
    return new Version(
      Integer.parseInt(matcher.group(1)),
      Integer.parseInt(matcher.group(2)),
      Integer.parseInt(matcher.group(3)),
      matcher.group(4));
  }

  class ByNameInference implements DependencyVersionInference {

    final Pattern pattern;
    final String groupId;
    final String artifactId;

    protected ByNameInference(Pattern pattern, String groupId, String artifactId) {
      this.pattern = pattern;
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    @Override
    public boolean handles(String groupId, String artifactId) {
      return groupId.equals(this.groupId) && artifactId.equals(this.artifactId);
    }

    @Override
    public Optional<DependencyVersion> infer(List<File> classpath) {
      for (File file : classpath) {
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.matches()) {
          return Optional.of(new DependencyVersionImpl(
            groupId, artifactId, matcherToVersion(matcher)));
        }
      }
      return Optional.empty();
    }
  }

  class ReflectiveInference implements DependencyVersionInference {
    private static final String KNOWN_CLASS_NAME = "lombok.Lombok";

    // TODO generalize for other libraries
    String groupId = "org.projectlombok";
    String artifactId = "lombok";
    @Override
    public boolean handles(String groupId, String artifactId) {
      return groupId.equals(groupId) && artifactId.equals(artifactId);
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

        return Optional.of(
          new DependencyVersionImpl(
            groupId,
            artifactId,
            matcherToVersion(VERSION_PATTERN.matcher(implementationVersion))));
      } catch (ClassNotFoundException e) {
        return Optional.empty();
      }
    }
  }

  class ManifestInference implements DependencyVersionInference {

    final String attributeName;
    final String groupId;
    final String artifactId;

    public ManifestInference(String attributeName, String groupId, String artifactId) {
      this.attributeName = attributeName;
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    @Override
    public boolean handles(String groupId, String artifactId) {
      return groupId.equals(this.groupId) && artifactId.equals(this.artifactId);
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
            Matcher matcher = VERSION_PATTERN.matcher(mainAttributes.getValue(attributeName));
            if (matcher.matches()) {
              return Optional.of(
                new DependencyVersionImpl(groupId, artifactId, matcherToVersion(matcher)));
            }
          }
        }
      } catch (IOException ignored) {
      }
      return Optional.empty();
    }
  }

}
