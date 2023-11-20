/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.classpath;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaSdkUtilTest {

  @Test
  void wrong_sdk_path_works() {
    Path path = new File("src/test/jdk/do-not-exists").toPath();
    assertThat(JavaSdkUtil.getJdkClassesRoots(path)).isEmpty();
  }

  @Test
  void should_find_jars_in_classic_jdk() {
    Path path = new File("src/test/jdk/jdk_classic").toPath();
    List<File> jdkClassesRoots = JavaSdkUtil.getJdkClassesRoots(path);
    assertThat(jdkClassesRoots).hasSize(1);
    assertThat(jdkClassesRoots.get(0)).hasName("rt.jar");
  }

  @Test
  void should_find_jars_in_modular_jdk() {
    Path path = new File("src/test/jdk/jdk_modular").toPath();
    List<File> jdkClassesRoots = JavaSdkUtil.getJdkClassesRoots(path);
    assertThat(jdkClassesRoots).hasSize(1);
    assertThat(jdkClassesRoots.get(0)).hasName("jrt-fs.jar");
  }

  @Test
  void should_find_jars_in_mac_jdk() {
    Path path = new File("src/test/jdk/jdk_mac").toPath();
    List<File> jdkClassesRoots = JavaSdkUtil.getJdkClassesRoots(path, true);
    assertThat(jdkClassesRoots).hasSize(1);
    assertThat(jdkClassesRoots.get(0)).hasName("rt.jar");
  }

  @Test
  void should_not_find_jars_in_mac_jdk_when_missconfigured() {
    Path path = new File("src/test/jdk/do-not-exists").toPath();
    List<File> jdkClassesRoots = JavaSdkUtil.getJdkClassesRoots(path, true);
    assertThat(jdkClassesRoots).isEmpty();
  }

  @Test
  void should_load_vm_jar_from_ibm_specific_dir_on_unix() {
    Path path = Paths.get("src", "test", "jdk", "ibm_jdk_8_linux");
    List<File> jdkClassRoots = JavaSdkUtil.getJdkClassesRoots(path);
    assertThat(jdkClassRoots).isNotEmpty();
    File expected = path.resolve(Paths.get("jre", "lib","amd64","default","jclSC180","vm.jar")).toAbsolutePath().toFile();
    assertThat(jdkClassRoots).contains(expected);
  }

  @Test
  void should_load_vm_jar_from_ibm_specific_dir_on_windows() {
    Path path = Paths.get("src", "test", "jdk", "ibm_jdk_8_windows");
    List<File> jdkClassRoots = JavaSdkUtil.getJdkClassesRoots(path);
    assertThat(jdkClassRoots).isNotEmpty();
    File expected = path.resolve(Paths.get("bin","default","jclSC180","vm.jar")).toAbsolutePath().toFile();
    assertThat(jdkClassRoots).contains(expected);
  }

  @Test
  void collect_jars_from_classpath_file() {
    List<File> actual = JavaSdkUtil.collectJarsFromClasspathFile("src/test/resources/classpath-example.txt");
    assertThat(actual).hasSize(1);
    File file = actual.get(0);
    assertThat(file.toString().replace(File.separatorChar, '/'))
      .endsWith("/org/openjdk/jol/jol-core/0.16/jol-core-0.16.jar");
  }

  @Test
  void collect_jars_from_missing_classpath_file() {
    assertThatThrownBy(() -> JavaSdkUtil.collectJarsFromClasspathFile("src/test/resources/missing-file.txt"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("missing-file.txt");
  }

  @Test
  void collect_jars_from_classpath_file_with_invalid_entries() {
    assertThatThrownBy(() -> JavaSdkUtil.collectJarsFromClasspathFile("src/test/resources/invalid-classpath-example.txt"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("missing-artifact-666.jar");
  }

  @Test
  void maven_local_repository_without_M2_REPO() {
    UnaryOperator<String> systemPropertyProvider = Map.of("user.home", File.separatorChar + "Users" + File.separatorChar + "me")::get;
    UnaryOperator<String> systemEnvProvider = Collections.<String, String>emptyMap()::get;
    assertThat(JavaSdkUtil.getMavenLocalRepository(systemEnvProvider, systemPropertyProvider))
      .isEqualTo("/Users/me/.m2/repository".replace('/', File.separatorChar));
  }

  @Test
  void maven_local_repository_with_blank_M2_REPO() {
    UnaryOperator<String> systemPropertyProvider = Map.of("user.home", File.separatorChar + "Users" + File.separatorChar + "me")::get;
    UnaryOperator<String> systemEnvProvider = Map.of("M2_REPO", "")::get;
    assertThat(JavaSdkUtil.getMavenLocalRepository(systemEnvProvider, systemPropertyProvider))
      .isEqualTo("/Users/me/.m2/repository".replace('/', File.separatorChar));
  }

  @Test
  void maven_local_repository_with_valid_M2_REPO() {
    UnaryOperator<String> systemPropertyProvider = Map.of("user.home", File.separatorChar + "Users" + File.separatorChar + "me")::get;
    String fooRepo = "/home/foo/.m2/repository".replace('/', File.separatorChar);
    UnaryOperator<String> systemEnvProvider = Map.of("M2_REPO", fooRepo)::get;
    assertThat(JavaSdkUtil.getMavenLocalRepository(systemEnvProvider, systemPropertyProvider))
      .isEqualTo(fooRepo);
  }

}
