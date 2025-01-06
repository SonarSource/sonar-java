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
package org.sonar.plugins.surefire.api;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;

class SurefireUtilsTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void should_get_report_paths_from_property() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.junit.reportPaths", "target/surefire,submodule/target/surefire");

    DefaultFileSystem fs = new DefaultFileSystem(new File("src/test/resources/org/sonar/plugins/surefire/api/SurefireUtilsTest/shouldGetReportsPathFromProperty"));
    PathResolver pathResolver = new PathResolver();

    assertThat(logTester.logs(Level.INFO)).isEmpty();

    List<File> directories = SurefireUtils.getReportsDirectories(settings.asConfig(), fs, pathResolver);

    assertThat(directories).hasSize(2);
    File directory1 = directories.get(0);
    assertThat(directory1)
      .exists()
      .isDirectory();
    File directory2 = directories.get(1);
    assertThat(directory2)
      .exists()
      .isDirectory();
    assertThat(logTester.logs(Level.INFO)).isEmpty();
  }

  @Test
  void return_default_value_if_property_unset() throws Exception {
    MapSettings settings = new MapSettings();
    DefaultFileSystem fs = new DefaultFileSystem(new File("src/test/resources/org/sonar/plugins/surefire/api/SurefireUtilsTest"));
    PathResolver pathResolver = new PathResolver();

    assertThat(logTester.logs(Level.INFO)).isEmpty();

    List<File> directories = SurefireUtils.getReportsDirectories(settings.asConfig(), fs, pathResolver);

    assertThat(directories).hasSize(1);
    File directory = directories.get(0);
    assertThat(directory.getCanonicalPath()).endsWith("target"+File.separator+"surefire-reports");
    assertThat(directory).doesNotExist();
    assertThat(directory.isDirectory()).isFalse();
    assertThat(logTester.logs(Level.INFO)).isEmpty();
  }
}
