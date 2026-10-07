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
package org.sonar.java.classpath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisWarningsWrapper;
import org.sonar.java.TestUtils;
import org.sonar.scanner.extension.PropertyDefinitions;
import org.sonar.scanner.plugin.api.impl.config.MapSettings;
import org.sonar.scanner.plugin.api.impl.fs.DefaultFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.java.classpath.ClasspathProperties.SONAR_JAVA_SOURCEPATH;
import static org.sonar.java.classpath.ClasspathProperties.SONAR_JAVA_TEST_SOURCEPATH;

class SourcePathTest {

  @TempDir
  Path temp;

  @Test
  void resolves_and_deduplicates_roots_with_the_analysis_encoding() throws Exception {
    Path first = Files.createDirectory(temp.resolve("first"));
    Path second = Files.createDirectory(temp.resolve("second"));
    var fs = new DefaultFileSystem(temp).setEncoding(StandardCharsets.ISO_8859_1);
    var settings = settings();
    settings.setProperty(SONAR_JAVA_SOURCEPATH, new String[] {"first", second.toString(), "first/../first"});
    SourcePath sourcePath = SourcePath.resolve(settings.asConfig(), fs, SONAR_JAVA_SOURCEPATH);
    assertThat(sourcePath.roots()).containsExactly(first.toFile(), second.toFile());
    assertThat(sourcePath.encodings()).containsExactly("ISO-8859-1", "ISO-8859-1");
  }

  @Test
  void rejects_missing_roots_and_files() throws Exception {
    Files.writeString(temp.resolve("file.java"), "class Example {}");
    for (String root : List.of("missing", "file.java")) {
      var settings = settings();
      settings.setProperty(SONAR_JAVA_SOURCEPATH, root);
      assertThatThrownBy(() -> SourcePath.resolve(settings.asConfig(), new DefaultFileSystem(temp), SONAR_JAVA_SOURCEPATH))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(SONAR_JAVA_SOURCEPATH)
        .hasMessageContaining("source root is not a directory");
    }
  }

  @Test
  void combines_source_paths_in_order() {
    var first = temp.resolve("first").toFile();
    var second = temp.resolve("second").toFile();
    SourcePath main = new SourcePath(List.of(first), List.of("UTF-8"));
    SourcePath test = new SourcePath(List.of(second, first), List.of("UTF-8", "UTF-8"));
    assertThat(SourcePath.combine(main, test)).isEqualTo(new SourcePath(List.of(first, second), List.of("UTF-8", "UTF-8")));
    assertThatThrownBy(() -> new SourcePath(List.of(first), List.of())).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void main_and_test_paths_are_independent_and_remove_only_the_missing_binary_warning() throws Exception {
    Path mainRoot = Files.createDirectory(temp.resolve("main"));
    Path testRoot = Files.createDirectory(temp.resolve("test"));
    var fs = new DefaultFileSystem(temp);
    fs.add(TestUtils.emptyInputFile("First.java"));
    fs.add(TestUtils.emptyInputFile("Second.java"));
    fs.add(TestUtils.emptyInputFile("FirstTest.java", InputFile.Type.TEST));
    fs.add(TestUtils.emptyInputFile("SecondTest.java", InputFile.Type.TEST));

    var settings = settings()
      .setProperty(SONAR_JAVA_SOURCEPATH, "main")
      .setProperty(SONAR_JAVA_TEST_SOURCEPATH, "test");

    List<String> warnings = new ArrayList<>();
    var analysisWarnings = new AnalysisWarningsWrapper(warnings::add);
    var main = new ClasspathForMain(settings.asConfig(), fs, analysisWarnings);
    var test = new ClasspathForTest(settings.asConfig(), fs, analysisWarnings);
    assertThat(main.getSourcePath().roots()).containsExactly(mainRoot.toFile());
    assertThat(test.getSourcePath().roots()).containsExactly(testRoot.toFile());
    assertThat(main.getElements()).isEmpty();
    assertThat(test.getElements()).isEmpty();
    main.logClasspathWarnings();
    test.logClasspathWarnings();
    assertThat(warnings).containsExactly(
      "Missing 'sonar.java.libraries' property. You might end up with less precise analysis results.",
      "Missing 'sonar.java.test.libraries' property. You might end up with less precise analysis results.");
  }

  private static MapSettings settings() {
    var definitions = new PropertyDefinitions();
    ClasspathProperties.getProperties().forEach(definitions::addComponent);
    return new MapSettings(definitions);
  }

}
