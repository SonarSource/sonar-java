/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.jsp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableRuleMigrationSupport
class JasperTest {

  Path tempFolder;
  Path webInf;

  @TempDir
  Path workDir;

  @Rule
  public LogTester logTester = new LogTester();

  @BeforeEach
  void setUp() throws Exception {
    // on macOS tmp is symbolic link which doesn't work well with Jasper, so we create tmp in 'target/tmp'
    tempFolder = Paths.get("target/tmp");
    Files.createDirectories(tempFolder);
    webInf = tempFolder.resolve("WEB-INF");
    Files.createDirectory(webInf);
  }

  @AfterEach
  void tearDown() throws Exception {
    FileUtils.deleteDirectory(tempFolder.toFile());
  }

  @Test
  void test_empty() throws Exception {
    SensorContextTester ctx = SensorContextTester.create(tempFolder);
    ctx.fileSystem().setWorkDir(workDir);
    List<InputFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(generatedFiles).isEmpty();
    assertThat(logTester.logs()).containsOnly("Found 0 JSP files.");
  }

  @Test
  void test_compilation() throws Exception {
    SensorContextTester ctx = jspContext("<html>\n" +
      "<body>\n" +
      "<h2>Hello World!</h2>\n" +
      "</body>\n" +
      "</html>");
    List<InputFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());

    assertThat(generatedFiles).hasSize(1);
    InputFile generatedFile = generatedFiles.iterator().next();
    List<String> generatedCode = Files.readAllLines(generatedFile.path());
    assertThat(generatedCode).contains("      out.write(\"<html>\\n<body>\\n<h2>Hello World!</h2>\\n</body>\\n</html>\");");
  }

  @Test
  void test_exception_handling() throws Exception {
    SensorContextTester ctx = jspContext("<%=");
    List<InputFile> inputFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(inputFiles).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Failed to transpile JSP files.");
  }

  @Test
  void test_walk() {
    assertThatThrownBy(() -> Jasper.walk(Paths.get("nonexistant")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Failed to walk nonexistant");
  }

  @Test
  void test_outputdir() throws Exception {
    SensorContextTester ctx = SensorContextTester.create(tempFolder);
    ctx.fileSystem().setWorkDir(workDir);
    Files.createFile(workDir.resolve("jsp"));
    assertThatThrownBy(() -> Jasper.outputDir(ctx))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to create output dir for jsp files");
  }

  private SensorContextTester jspContext(String jspSource) throws IOException {
    Path jsp = createJspFile(jspSource);
    SensorContextTester ctx = SensorContextTester.create(tempFolder);
    DefaultInputFile inputFile = TestInputFileBuilder.create("", tempFolder.toFile(), jsp.toFile())
      .setLanguage("jsp")
      .setContents(jspSource)
      .build();
    ctx.fileSystem().add(inputFile);
    ctx.fileSystem().setWorkDir(workDir);
    return ctx;
  }

  private Path createJspFile(String content) throws IOException {
    Path path = webInf.resolve("test.jsp");
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    return path;
  }

}
