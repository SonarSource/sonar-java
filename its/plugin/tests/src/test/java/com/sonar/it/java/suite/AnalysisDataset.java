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
package com.sonar.it.java.suite;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

record AnalysisDataset(String id, String label, String description, String sourceRoot,
                       Map<String, String> sharedProperties, List<Path> libraries, boolean expectCompilationSuccess) {

  AnalysisDataset {
    sharedProperties = Map.copyOf(sharedProperties);
    libraries = validateLibraries(libraries);
  }

  static List<AnalysisDataset> forProject(RepositoryScope scope, List<Path> libraries) throws IOException {
    return datasets(scope.name(), scope.sourceRoots(), externalLibraries(libraries, scope.projectArtifactIds()));
  }

  private static List<AnalysisDataset> datasets(String name, List<String> sourceRoots, List<Path> libraries) {
    if (libraries.isEmpty()) {
      throw new IllegalArgumentException("The full-classpath datasets require the project's compile-scope dependency JARs");
    }
    var datasets = new ArrayList<AnalysisDataset>();
    for (boolean dependencies : List.of(false, true)) {
      for (boolean fileByFile : List.of(false, true)) {
        String batching = fileByFile ? "file-by-file" : "normal";
        String classpath = dependencies ? "dependencies" : "no-libraries";
        String description = fileByFile ? "Each production file is parsed separately" : "Production files use the normal parser batch size";
        description += dependencies ? "; every mode receives the same compile-scope external JARs" : "; no external dependency JARs are supplied";
        datasets.add(new AnalysisDataset(name + "-" + batching + "-" + classpath,
          name + " / " + batching + " / " + classpath, description + "; native Maven module scope", String.join(",", sourceRoots),
          Map.of("sonar.java.fileByFile", Boolean.toString(fileByFile), "sonar.java.experimental.batchModeSizeInKB", "500"),
          dependencies ? libraries : List.of(), false));
      }
    }
    return List.copyOf(datasets);
  }

  record LibraryResolution(Map<String, List<Path>> byModule, List<Path> union) {
    LibraryResolution {
      var modules = new LinkedHashMap<String, List<Path>>();
      byModule.forEach((module, libraries) -> modules.put(module, List.copyOf(libraries)));
      byModule = Collections.unmodifiableMap(modules);
      union = List.copyOf(union);
      var paths = new HashSet<>(union);
      if (paths.size() != union.size() || byModule.values().stream().flatMap(List::stream).anyMatch(path -> !paths.contains(path))) {
        throw new IllegalArgumentException("Each module classpath must belong to a unique external library union");
      }
    }
  }

  static LibraryResolution resolveProjectLibraries(Path checkout, Path workspace, String mavenBinary, RepositoryScope scope) throws IOException {
    Path pom = RepositoryScope.copyReactorPoms(checkout, workspace);
    Path log = workspace.resolve("dependency-resolution.log").toAbsolutePath();
    var process = new ProcessBuilder(mavenBinary, "-B", "-ntp", "-f", pom.toString(), "-pl", String.join(",", scope.modulePaths()),
      "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath", "-DincludeScope=compile", "-Dmdep.outputFile=compile-classpath.txt")
      .directory(workspace.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    waitForResolution(process, log);
    var modules = new LinkedHashMap<String, List<Path>>();
    var union = new LinkedHashSet<Path>();
    var excluded = new LinkedHashSet<Path>();
    for (String module : scope.modulePaths()) {
      Path output = workspace.resolve(module).resolve("compile-classpath.txt");
      List<Path> resolved = readResolvedClasspath(output);
      List<Path> external = externalLibraries(resolved, scope.projectArtifactIds());
      modules.put(module, external);
      union.addAll(external);
      resolved.stream().filter(path -> !external.contains(path)).forEach(excluded::add);
    }
    Files.write(workspace.resolve("excluded-project-artifacts.txt"), excluded.stream().map(Path::toString).toList());
    return new LibraryResolution(modules, List.copyOf(union));
  }

  private static void waitForResolution(Process process, Path log) throws IOException {
    try {
      if (!process.waitFor(180, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IOException("Compile-scope dependency resolution timed out; see " + log);
      }
      if (process.exitValue() != 0) {
        throw new IOException("Compile-scope dependency resolution failed without compiling the project; see " + log);
      }
    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while resolving compile-scope dependencies", e);
    }
  }

  private static List<Path> readResolvedClasspath(Path output) throws IOException {
    String value = Files.readString(output, StandardCharsets.UTF_8).strip();
    if (value.isEmpty()) {
      return List.of();
    }
    return Pattern.compile(Pattern.quote(File.pathSeparator)).splitAsStream(value).map(Path::of).map(path -> path.toAbsolutePath().normalize()).toList();
  }

  static List<Path> externalLibraries(List<Path> libraries, Set<String> projectArtifactIds) throws IOException {
    var external = new LinkedHashSet<Path>();
    for (Path library : libraries) {
      Path path = library.toAbsolutePath().normalize();
      if (!isProjectLibrary(path, projectArtifactIds)) {
        external.add(path);
      }
    }
    return validateLibraries(List.copyOf(external));
  }

  private static boolean isProjectLibrary(Path library, Set<String> projectArtifactIds) throws IOException {
    if (projectRepositoryArtifact(library)) {
      return true;
    }
    String basename = library.getFileName().toString();
    if (projectArtifactIds.stream().anyMatch(artifact -> basename.startsWith(artifact + "-"))) {
      return true;
    }
    try (var jar = new JarFile(library.toFile())) {
      var entries = jar.entries();
      while (entries.hasMoreElements()) {
        var entry = entries.nextElement();
        if (entry.getName().startsWith("META-INF/maven/") && entry.getName().endsWith("/pom.properties")) {
          Properties coordinates = new Properties();
          try (var input = jar.getInputStream(entry)) {
            coordinates.load(input);
          }
          if (coordinates.getProperty("groupId", "").equals("org.sonarsource.java")
            && !coordinates.getProperty("artifactId", "").equals("jdt-package")) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean projectRepositoryArtifact(Path library) {
    String path = library.toString().replace('\\', '/');
    return path.contains("/org/sonarsource/java/") && !path.contains("/org/sonarsource/java/jdt-package/");
  }

  static List<Path> validateLibraries(List<Path> libraries) {
    var paths = new ArrayList<Path>();
    var unique = new HashSet<Path>();
    for (Path library : libraries) {
      Path path = library.toAbsolutePath().normalize();
      if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
        throw new IllegalArgumentException("Only existing external dependency JARs may be supplied: " + path);
      }
      if (projectRepositoryArtifact(path)) {
        throw new IllegalArgumentException("The target project's compiled artifact must not be supplied: " + path);
      }
      if (!unique.add(path)) {
        throw new IllegalArgumentException("The compile classpath contains a duplicate dependency: " + path);
      }
      paths.add(path);
    }
    return List.copyOf(paths);
  }

  static List<Path> freezeLibraries(List<Path> dependencies, Path directory) throws IOException {
    var originals = validateLibraries(dependencies);
    var frozen = new ArrayList<Path>();
    for (int i = 0; i < originals.size(); i++) {
      Path original = originals.get(i);
      Path parent = directory.resolve(String.format(Locale.ROOT, "%02d", i));
      Files.createDirectories(parent);
      Path copy = parent.resolve(original.getFileName());
      Files.copy(original, copy);
      frozen.add(copy.toAbsolutePath().normalize());
    }
    return List.copyOf(frozen);
  }

  static LibraryResolution freezeLibraries(LibraryResolution resolution, Path directory) throws IOException {
    var frozen = freezeLibraries(resolution.union(), directory);
    var replacements = new LinkedHashMap<Path, Path>();
    for (int i = 0; i < frozen.size(); i++) {
      replacements.put(resolution.union().get(i).toAbsolutePath().normalize(), frozen.get(i));
    }
    var modules = new LinkedHashMap<String, List<Path>>();
    resolution.byModule().forEach((module, libraries) -> modules.put(module,
      libraries.stream().map(path -> replacements.get(path.toAbsolutePath().normalize())).toList()));
    return new LibraryResolution(modules, frozen);
  }
}
