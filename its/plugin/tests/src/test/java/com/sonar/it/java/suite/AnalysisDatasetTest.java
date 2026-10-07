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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisDatasetTest {

  @TempDir
  Path directory;

  @Test
  void covers_batching_and_classpath_independently_with_the_same_analyzed_scope() throws IOException {
    Path library = libraryJar(directory.resolve("external.jar"), "external", "library");
    var scope = RepositoryScope.discover(sampleRepository());
    var datasets = AnalysisDataset.forProject(scope, List.of(library));
    assertThat(datasets).hasSize(4);
    assertThat(datasets.stream().map(AnalysisDataset::id)).doesNotHaveDuplicates();
    assertThat(datasets).allSatisfy(dataset -> {
      assertThat(dataset.sourceRoot()).isEqualTo(String.join(",", scope.sourceRoots()));
      assertThat(dataset.sharedProperties()).containsOnlyKeys("sonar.java.fileByFile", "sonar.java.experimental.batchModeSizeInKB")
        .containsEntry("sonar.java.experimental.batchModeSizeInKB", "500");
      if (dataset.id().endsWith("-dependencies")) {
        assertThat(dataset.libraries()).containsExactly(library);
      } else {
        assertThat(dataset.libraries()).isEmpty();
      }
    });
    assertThat(datasets.stream().map(dataset -> dataset.sharedProperties().get("sonar.java.fileByFile")))
      .containsExactly("false", "true", "false", "true");
    assertThatThrownBy(() -> AnalysisDataset.forProject(scope, List.of())).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("compile-scope dependency JARs");
  }

  @Test
  void snapshots_properties_and_classpaths_so_modes_cannot_mutate_other_inputs() throws IOException {
    var properties = new HashMap<>(Map.of("sonar.java.fileByFile", "true"));
    Path library = Files.writeString(directory.resolve("external.jar"), "external");
    var libraries = new ArrayList<>(List.of(library));
    var dataset = new AnalysisDataset("id", "label", "description", "src", properties, libraries, true);
    properties.clear();
    libraries.clear();
    assertThat(dataset.sharedProperties()).containsExactlyEntriesOf(Map.of("sonar.java.fileByFile", "true"));
    assertThat(dataset.libraries()).containsExactly(library);
    assertThatThrownBy(() -> dataset.sharedProperties().put("other", "value")).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> dataset.libraries().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejects_missing_paths_class_directories_duplicate_jars_and_the_target_artifact() throws IOException {
    Path library = Files.writeString(directory.resolve("external.jar"), "external");
    Path classDirectory = Files.createDirectory(directory.resolve("classes"));
    Path targetArtifact = cachedProjectArtifact();
    for (Path invalid : List.of(directory.resolve("missing.jar"), classDirectory)) {
      assertThatThrownBy(() -> AnalysisDataset.validateLibraries(List.of(invalid))).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("existing external dependency JARs");
    }
    assertThatThrownBy(() -> AnalysisDataset.validateLibraries(List.of(library, library))).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("duplicate dependency");
    assertThatThrownBy(() -> AnalysisDataset.validateLibraries(List.of(targetArtifact))).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("target project's compiled artifact");
  }

  @Test
  void freezes_library_bytes_and_classpath_order_without_changing_artifact_basenames() throws IOException {
    Path firstDirectory = Files.createDirectory(directory.resolve("first"));
    Path secondDirectory = Files.createDirectory(directory.resolve("second"));
    String basename = "spring-context-6.2.0.jar";
    Path first = Files.write(firstDirectory.resolve(basename), new byte[] {1, 2});
    Path second = Files.write(secondDirectory.resolve(basename), new byte[] {3, 4});

    var frozen = AnalysisDataset.freezeLibraries(List.of(second, first), directory.resolve("frozen"));

    assertThat(frozen).hasSize(2);
    assertThat(frozen.stream().map(path -> path.getFileName().toString())).containsExactly(basename, basename);
    assertThat(frozen.getFirst()).isNotEqualTo(frozen.getLast());
    assertThat(Files.readAllBytes(frozen.getFirst())).containsExactly(3, 4);
    assertThat(Files.readAllBytes(frozen.getLast())).containsExactly(1, 2);
    Files.write(second, new byte[] {9});
    assertThat(Files.readAllBytes(frozen.getFirst())).containsExactly(3, 4);
    assertThatThrownBy(() -> frozen.clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejects_target_bytecode_before_creating_a_frozen_classpath() throws IOException {
    Path external = Files.writeString(directory.resolve("external-1.0.jar"), "external");
    Path target = cachedProjectArtifact();
    Path frozen = directory.resolve("frozen");

    assertThatThrownBy(() -> AnalysisDataset.freezeLibraries(List.of(external, target), frozen))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("target project's compiled artifact");
    assertThat(frozen).doesNotExist();
  }

  @Test
  void discovers_production_modules_and_positive_classpath_tooling_without_indexing_fixtures_or_generated_sources() throws IOException {
    Path checkout = sampleRepository();

    var scope = RepositoryScope.discover(checkout);

    assertThat(scope.name()).isEqualTo("sonar-java");
    assertThat(scope.modulePaths()).containsExactly("engine", "java-checks-testkit", "java-checks-test-sources/test-classpath-reader");
    assertThat(scope.projectArtifactIds()).containsExactlyInAnyOrder("engine", "java-checks-testkit", "test-classpath-reader");
    assertThat(scope.expectedFiles()).containsExactly("engine/src/main/java/p/Engine.java",
      "java-checks-test-sources/test-classpath-reader/src/main/java/p/Reader.java", "java-checks-testkit/src/main/java/p/Verifier.java");
    assertThat(scope.sourceRoots()).containsExactly("engine/src/main/java", "java-checks-testkit/src/main/java",
      "java-checks-test-sources/test-classpath-reader/src/main/java");
    assertThatThrownBy(() -> scope.modules().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void copied_reactor_keeps_parent_poms_without_build_outputs_or_source_files() throws IOException {
    Path checkout = sampleRepository();
    Path copy = directory.resolve("copy");

    RepositoryScope.copyReactorPoms(checkout, copy);

    assertThat(copy.resolve("java-checks-test-sources/pom.xml")).isRegularFile();
    assertThat(copy.resolve("engine/pom.xml")).isRegularFile();
    assertThat(copy.resolve("engine/src/main/java")).doesNotExist();
    assertThat(copy.resolve("engine/target")).doesNotExist();
    assertThat(checkout.resolve("engine/target/classes/Existing.class")).isRegularFile();
    assertThatThrownBy(() -> RepositoryScope.copyReactorPoms(checkout, checkout.resolve("target/copy")))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("outside the target checkout");
  }

  @Test
  void excludes_cached_project_artifacts_by_maven_coordinates_and_names_while_retaining_external_jdt() throws IOException {
    Path api = libraryJar(directory.resolve("api-1.jar"), "org.sonarsource.api.plugin", "sonar-plugin-api");
    Path hiddenProject = libraryJar(directory.resolve("renamed.jar"), "org.sonarsource.java", "java-frontend");
    Path namedProject = libraryJar(directory.resolve("engine-8.45.jar"), "external", "something");
    Path jdt = libraryJar(directory.resolve("jdt-package-1.9.jar"), "org.sonarsource.java", "jdt-package");

    assertThat(AnalysisDataset.externalLibraries(List.of(api, hiddenProject, namedProject, jdt, api), Set.of("engine", "java-frontend")))
      .containsExactly(api, jdt);
  }

  @Test
  void freezing_preserves_distinct_module_dependency_versions_and_classpath_order() throws IOException {
    Path firstVersion = libraryJar(directory.resolve("library-1.jar"), "external", "library");
    Path secondVersion = libraryJar(directory.resolve("library-2.jar"), "external", "library");
    var resolution = new AnalysisDataset.LibraryResolution(Map.of("first", List.of(firstVersion), "second", List.of(secondVersion, firstVersion)),
      List.of(firstVersion, secondVersion));

    var frozen = AnalysisDataset.freezeLibraries(resolution, directory.resolve("frozen"));

    assertThat(frozen.byModule().get("first")).containsExactly(frozen.union().getFirst());
    assertThat(frozen.byModule().get("second")).containsExactly(frozen.union().getLast(), frozen.union().getFirst());
    assertThat(frozen.byModule().get("second").stream().map(path -> path.getFileName().toString())).containsExactly("library-2.jar", "library-1.jar");
    assertThatThrownBy(() -> frozen.byModule().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> frozen.byModule().get("first").clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> new AnalysisDataset.LibraryResolution(Map.of("first", List.of(firstVersion)), List.of(secondVersion)))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unique external library union");
  }

  @Test
  void sonar_java_factory_uses_all_native_module_roots_and_does_not_assume_compilation_success() throws IOException {
    var scope = RepositoryScope.discover(sampleRepository());
    Path library = libraryJar(directory.resolve("external-1.jar"), "external", "library");

    var datasets = AnalysisDataset.forProject(scope, List.of(library));

    assertThat(datasets).hasSize(4).allSatisfy(dataset -> {
      assertThat(dataset.label()).startsWith("sonar-java / ");
      assertThat(dataset.sourceRoot()).isEqualTo(String.join(",", scope.sourceRoots()));
      assertThat(dataset.description()).contains("native Maven module scope");
      assertThat(dataset.expectCompilationSuccess()).isFalse();
    });
    assertThat(datasets.getFirst().libraries()).isEmpty();
    assertThat(datasets.getLast().libraries()).containsExactly(library);
  }

  private Path cachedProjectArtifact() throws IOException {
    Path artifact = directory.resolve("org/sonarsource/java/java-frontend/8.45/java-frontend-8.45.jar");
    Files.createDirectories(artifact.getParent());
    return Files.writeString(artifact, "compiled-project-bytecode");
  }

  private Path sampleRepository() throws IOException {
    Path checkout = directory.resolve("repository");
    Files.createDirectories(checkout);
    Files.writeString(checkout.resolve("pom.xml"), """
      <project><parent><artifactId>parent</artifactId></parent><artifactId>java</artifactId><modules>
      <module>engine</module><module>java-checks-testkit</module><module>docs</module><module>its</module><module>java-checks-test-sources</module>
      </modules></project>
      """);
    repositoryModule(checkout, "engine", "engine", "Engine.java");
    repositoryModule(checkout, "java-checks-testkit", "java-checks-testkit", "Verifier.java");
    repositoryModule(checkout, "java-checks-test-sources/test-classpath-reader", "test-classpath-reader", "Reader.java");
    repositoryModule(checkout, "java-checks-test-sources/default", "default", "Negative.java");
    repositoryModule(checkout, "docs", "docs", "Example.java");
    repositoryModule(checkout, "its", "its", "Fixture.java");
    Files.writeString(checkout.resolve("java-checks-test-sources/pom.xml"), "<project><artifactId>java-checks-test-sources</artifactId></project>");
    Files.createDirectories(checkout.resolve("engine/target/classes"));
    Files.writeString(checkout.resolve("engine/target/classes/Existing.class"), "existing-bytecode");
    Files.createDirectories(checkout.resolve("engine/target/generated-sources/p"));
    Files.writeString(checkout.resolve("engine/target/generated-sources/p/Generated.java"), "class Generated {}");
    return checkout;
  }

  private static void repositoryModule(Path checkout, String module, String artifact, String javaFile) throws IOException {
    Path directory = checkout.resolve(module);
    Files.createDirectories(directory.resolve("src/main/java/p"));
    Files.writeString(directory.resolve("pom.xml"), "<project><parent><artifactId>parent</artifactId></parent><artifactId>" + artifact + "</artifactId></project>");
    Files.writeString(directory.resolve("src/main/java/p").resolve(javaFile), "class Example {}");
  }

  private static Path libraryJar(Path path, String group, String artifact) throws IOException {
    try (var output = new JarOutputStream(Files.newOutputStream(path))) {
      output.putNextEntry(new JarEntry("META-INF/maven/" + group + "/" + artifact + "/pom.properties"));
      output.write(("groupId=" + group + "\nartifactId=" + artifact + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
    return path;
  }
}
