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
package org.sonar.java.checks.verifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class FilesUtilsTest {
  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  void verify_get_classpath_files() throws IOException {
    Path tmp = temp.newFolder().toPath();
    Path jar = tmp.resolve("test.jar");
    Path zip = tmp.resolve("test.zip");
    Path invalid = tmp.resolve("test.txt");

    Files.createFile(jar);
    Files.createFile(zip);
    Files.createFile(invalid);

    List<File> list = FilesUtils.getFilesRecursively(temp.getRoot().toPath(), new String[] {"zip", "jar"});
    assertThat(list).containsOnly(jar.toFile(), zip.toFile());
  }

}
