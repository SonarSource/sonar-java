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
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactorProjectTest {

  @TempDir
  Path directory;

  @Test
  void writes_native_module_parent_layout_without_indexing_aggregator_sources_or_changing_java_files() throws Exception {
    var scope = scope();
    Path project = directory.resolve("project");
    Path source = project.resolve("engine/src/main/java/p/Engine.java");
    Files.createDirectories(source.getParent());
    Files.writeString(source, "class Engine {}");

    ReactorProject.write(project, scope, Map.of(), "5.9.0.7291");

    Document parent = pom(project.resolve("pom.xml"));
    assertThat(text(parent, "packaging")).isEqualTo("pom");
    assertThat(parent.getElementsByTagName("module").getLength()).isEqualTo(2);
    assertThat(parent.getElementsByTagName("module").item(1).getTextContent()).isEqualTo("java-checks-test-sources/test-classpath-reader");
    assertThat(text(parent, "sonar.sources")).isEmpty();
    assertThat(text(parent, "sonar.java.libraries")).isEmpty();
    assertThat(text(pom(project.resolve("engine/pom.xml")), "relativePath")).isEqualTo("../pom.xml");
    Document nested = pom(project.resolve("java-checks-test-sources/test-classpath-reader/pom.xml"));
    assertThat(text(nested, "relativePath")).isEqualTo("../../pom.xml");
    assertThat(text(nested, "sonar.sources")).isEqualTo("src/main/java");
    assertThat(Files.readString(source)).isEqualTo("class Engine {}");
  }

  @Test
  void preserves_module_specific_versions_and_order_without_project_dependency_declarations() throws Exception {
    Path first = jar("library-1.jar", "external", "library");
    Path second = jar("library-2.jar", "external", "library");
    Path project = directory.resolve("project");
    var classpaths = Map.of("engine", List.of(first), "java-checks-test-sources/test-classpath-reader", List.of(second, first));

    ReactorProject.write(project, scope(), classpaths, "5.9.0.7291");

    Document engine = pom(project.resolve("engine/pom.xml"));
    Document tooling = pom(project.resolve("java-checks-test-sources/test-classpath-reader/pom.xml"));
    assertThat(text(engine, "sonar.java.libraries")).isEqualTo(first.toString());
    assertThat(text(tooling, "sonar.java.libraries")).isEqualTo(second + "," + first);
    assertThat(engine.getElementsByTagName("dependency").getLength()).isZero();
    assertThat(tooling.getElementsByTagName("dependency").getLength()).isZero();
    assertThat(pom(project.resolve("pom.xml")).getElementsByTagName("dependency").getLength()).isZero();
  }

  @Test
  void maintains_empty_supplied_binaries_tests_and_libraries_in_every_module() throws Exception {
    Path project = directory.resolve("project");
    ReactorProject.write(project, scope(), Map.of(), "5.9.0.7291");

    for (Path path : List.of(project.resolve("pom.xml"), project.resolve("engine/pom.xml"),
      project.resolve("java-checks-test-sources/test-classpath-reader/pom.xml"))) {
      Document model = pom(path);
      for (String property : List.of("sonar.tests", "sonar.java.binaries", "sonar.java.libraries", "sonar.java.test.binaries", "sonar.java.test.libraries")) {
        assertThat(text(model, property)).isEmpty();
      }
      assertThat(path.getParent().resolve("target/classes")).doesNotExist();
    }
    assertThat(text(pom(project.resolve("pom.xml")), "maven.compiler.release")).isEqualTo("21");
    assertThat(text(pom(project.resolve("pom.xml")), "project.build.sourceEncoding")).isEqualTo("UTF-8");
  }

  @Test
  void xml_escapes_paths_while_preserving_their_decoded_scanner_values() throws Exception {
    Path libraryDirectory = Files.createDirectory(directory.resolve("libraries & shared"));
    Path library = jarAt(libraryDirectory.resolve("library-1.jar"), "external", "library");
    var module = new RepositoryScope.Module("engine & helpers", "engine", List.of());
    var scope = new RepositoryScope("project", List.of(module));
    Path project = directory.resolve("project");

    ReactorProject.write(project, scope, Map.of(module.path(), List.of(library)), "5.9.0.7291");

    assertThat(Files.readString(project.resolve("pom.xml"))).contains("engine &amp; helpers");
    assertThat(Files.readString(project.resolve(module.path()).resolve("pom.xml"))).contains("libraries &amp; shared");
    assertThat(text(pom(project.resolve(module.path()).resolve("pom.xml")), "sonar.java.libraries")).isEqualTo(library.toString());
  }

  @Test
  void rejects_incomplete_external_configuration_and_compiled_target_artifacts() throws IOException {
    Path external = jar("library-1.jar", "external", "library");
    Path project = directory.resolve("project");
    assertThatThrownBy(() -> ReactorProject.write(project, scope(), Map.of("engine", List.of(external)), "5.9.0.7291"))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("every scoped module");
    assertThat(project).doesNotExist();

    Path own = jar("engine-1.jar", "org.sonarsource.java", "engine");
    assertThatThrownBy(() -> ReactorProject.write(project, scope(), Map.of("engine", List.of(own),
      "java-checks-test-sources/test-classpath-reader", List.of()), "5.9.0.7291"))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Compiled target-module artifacts");
    assertThat(project).doesNotExist();
  }

  private static RepositoryScope scope() {
    return new RepositoryScope("sonar-java", List.of(new RepositoryScope.Module("engine", "engine", List.of()),
      new RepositoryScope.Module("java-checks-test-sources/test-classpath-reader", "test-classpath-reader", List.of())));
  }

  private Path jar(String name, String group, String artifact) throws IOException {
    return jarAt(directory.resolve(name), group, artifact);
  }

  private static Path jarAt(Path path, String group, String artifact) throws IOException {
    try (var output = new JarOutputStream(Files.newOutputStream(path))) {
      output.putNextEntry(new JarEntry("META-INF/maven/" + group + "/" + artifact + "/pom.properties"));
      output.write(("groupId=" + group + "\nartifactId=" + artifact + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
      output.closeEntry();
    }
    return path;
  }

  private static Document pom(Path path) throws Exception {
    var factory = DocumentBuilderFactory.newDefaultInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return factory.newDocumentBuilder().parse(path.toFile());
  }

  private static String text(Document document, String key) {
    return document.getElementsByTagName(key).item(0).getTextContent();
  }
}
