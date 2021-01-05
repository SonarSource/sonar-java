/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
