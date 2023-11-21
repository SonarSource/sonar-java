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
package org.sonar.java.test.classpath;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

}
