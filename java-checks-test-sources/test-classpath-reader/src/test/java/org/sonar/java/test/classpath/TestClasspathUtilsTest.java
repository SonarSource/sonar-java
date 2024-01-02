/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.test.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.test.classpath.TestClasspathUtils.fixSeparator;
import static org.sonar.java.test.classpath.TestClasspathUtils.xmlNodeValue;

class TestClasspathUtilsTest {

  @Test
  void collect_jars_from_classpath_file() {
    List<File> actual = TestClasspathUtils.loadFromFile("src/test/resources/classpath-example.txt");
    assertThat(actual).hasSize(1);
    File file = actual.get(0);
    assertThat(file.toString().replace(File.separatorChar, '/'))
      .endsWith("/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar");
  }

  @Test
  void collect_jars_from_missing_classpath_file() {
    assertThatThrownBy(() -> TestClasspathUtils.loadFromFile("src/test/resources/missing-file.txt"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("missing-file.txt");
  }

  @Test
  void collect_jars_from_classpath_file_with_invalid_entries() {
    assertThatThrownBy(() -> TestClasspathUtils.loadFromFile("src/test/resources/invalid-classpath-example.txt"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("missing-artifact-666.jar");
  }

  @Test
  void maven_local_repository_without_M2_REPO() {
    UnaryOperator<String> systemPropertyProvider = Map.of("user.home", File.separatorChar + "Users" + File.separatorChar + "me")::get;
    UnaryOperator<String> systemEnvProvider = Collections.<String, String>emptyMap()::get;
    assertThat(TestClasspathUtils.findMavenLocalRepository(systemEnvProvider, systemPropertyProvider))
      .isEqualTo("/Users/me/.m2/repository".replace('/', File.separatorChar));
  }

  @Test
  void maven_local_repository_with_blank_M2_REPO() {
    UnaryOperator<String> systemPropertyProvider = Map.of("user.home", File.separatorChar + "Users" + File.separatorChar + "me")::get;
    UnaryOperator<String> systemEnvProvider = Map.of("M2_REPO", "")::get;
    assertThat(TestClasspathUtils.findMavenLocalRepository(systemEnvProvider, systemPropertyProvider))
      .isEqualTo("/Users/me/.m2/repository".replace('/', File.separatorChar));
  }

  @Test
  void maven_local_repository_with_valid_M2_REPO() {
    UnaryOperator<String> systemPropertyProvider = Map.of("user.home", File.separatorChar + "Users" + File.separatorChar + "me")::get;
    String fooRepo = "/home/foo/.m2/repository".replace('/', File.separatorChar);
    UnaryOperator<String> systemEnvProvider = Map.of("M2_REPO", fooRepo)::get;
    assertThat(TestClasspathUtils.findMavenLocalRepository(systemEnvProvider, systemPropertyProvider))
      .isEqualTo(fooRepo);
  }

  @Test
  void find_module_jar_in_target_folder(@TempDir Path moduleDir) throws IOException {
    writePomFile(moduleDir.resolve("pom.xml"),
      "  <parent>\n" +
      "    <groupId>org.example</groupId>\n" +
      "    <artifactId>my-parent</artifactId>\n" +
      "    <version>1.0</version>\n" +
      "  </parent>\n" +
      "\n" +
      "  <artifactId>my-artifact</artifactId>\n");
    Path target = moduleDir.resolve("target");
    Files.createDirectory(target);
    Path targetJarPath = target.resolve("my-artifact-1.0.jar");
    Files.createFile(targetJarPath);
    Path actual = TestClasspathUtils.findModuleJarPath(moduleDir.toString());
    assertThat(actual).isEqualTo(targetJarPath.toRealPath());
  }

  @Test
  void find_module_jar_in_local_repo(@TempDir Path moduleDir) throws IOException {
    writePomFile(moduleDir.resolve("pom.xml"),
      "  <groupId>com.google.code.findbugs</groupId>\n" +
        "  <artifactId>jsr305</artifactId>\n" +
        "  <version>3.0.2</version>\n");
    Path actual = TestClasspathUtils.findModuleJarPath(moduleDir.toString());
    assertThat(actual.toString()).endsWith(fixSeparator("/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar"));
  }

  @Test
  void find_module_jar_without_pom(@TempDir Path moduleDir) {
    String moduleDirPath = moduleDir.toString();
    assertThatThrownBy(() -> TestClasspathUtils.findModuleJarPath(moduleDirPath))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Exception reading")
      .hasMessageContaining(fixSeparator("pom.xml"));
  }

  @Test
  void find_module_jar_without_jar(@TempDir Path moduleDir) throws IOException {
    writePomFile(moduleDir.resolve("pom.xml"),
      "  <parent>\n" +
        "    <groupId>org.example</groupId>\n" +
        "    <artifactId>my-parent</artifactId>\n" +
        "    <version>1.0</version>\n" +
        "  </parent>\n" +
        "\n" +
        "  <groupId>org.example.error</groupId>\n" +
        "  <artifactId>missing-artifact</artifactId>\n" +
        "  <version>42.0</version>\n");
    String moduleDirPath = moduleDir.toString();
    assertThatThrownBy(() -> TestClasspathUtils.findModuleJarPath(moduleDirPath))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Missing jar for module")
      .hasMessageContaining(fixSeparator( "target/missing-artifact-1.0.jar"))
      .hasMessageContaining(fixSeparator( "/org/example/missing-artifact/1.0/missing-artifact-1.0.jar"));
  }

  @Test
  void find_module_jar_in_invalid_folder() throws IOException {
    assertThatThrownBy(() -> TestClasspathUtils.findModuleJarPath("/invalid/path/to/module"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Module path exception '/invalid/path/to/module'");
  }

  @Test
  void read_xml_node_value(@TempDir Path moduleDir) throws IOException {
    Path pomPath = moduleDir.resolve("pom.xml");
    writePomFile(pomPath,
      "  <parent>\n" +
        "    <groupId>org.example</groupId>\n" +
        "    <artifactId>my-parent</artifactId>\n" +
        "    <version>1.0</version>\n" +
        "  </parent>\n" +
        "  <artifactId>my-artifact</artifactId>\n");

    Document xml = TestClasspathUtils.loadXml(pomPath);
    assertThat(xmlNodeValue(xml, "/project/artifactId/text()")).isEqualTo("my-artifact");
    assertThat(xmlNodeValue(xml, "/project/groupId/text()|/project/parent/groupId/text()")).isEqualTo("org.example");
    assertThatThrownBy(() -> xmlNodeValue(xml, "/project/missing/text()"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Missing node for xpath '/project/missing/text()'");
    assertThatThrownBy(() -> xmlNodeValue(xml, ") invalid xpath expression ("))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Exception evaluating ') invalid xpath expression ('");
  }

  private static void writePomFile(Path path, String projectContent) throws IOException {
    Files.writeString(path,
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
      "  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
      "  <modelVersion>4.0.0</modelVersion>\n" +
      projectContent + "\n" +
      "</project>\n",
      UTF_8);
  }

}
