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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
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
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.model.GeneratedFile;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@EnableRuleMigrationSupport
class JasperTest {

  private static final String JSP_SOURCE = "<html>\n" +
    "<body>\n" +
    "<h2>Hello World!</h2>\n" +
    "</body>\n" +
    "</html>";

  Path tempFolder;
  Path webInf;

  @TempDir
  Path workDir;

  @Rule
  public LogTester logTester = new LogTester();
  private Path jspFile;

  @BeforeEach
  void setUp() throws Exception {
    // on macOS tmp is symbolic link which doesn't work well with Jasper, so we create tmp in 'target/tmp'
    tempFolder = Paths.get("target/tmp");
    webInf = tempFolder.resolve("src/main/webapp/WEB-INF");
    Files.createDirectories(webInf);
  }

  @AfterEach
  void tearDown() throws Exception {
    FileUtils.deleteDirectory(tempFolder.toFile());
  }

  @Test
  void test_empty() throws Exception {
    SensorContextTester ctx = SensorContextTester.create(tempFolder);
    ctx.fileSystem().setWorkDir(workDir);
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(generatedFiles).isEmpty();
    assertThat(logTester.logs()).containsOnly("Found 0 JSP files.");
  }

  @Test
  void test_compilation() throws Exception {
    SensorContextTester ctx = jspContext(JSP_SOURCE);
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());

    assertThat(generatedFiles).hasSize(1);
    InputFile generatedFile = generatedFiles.iterator().next();
    List<String> generatedCode = Files.readAllLines(generatedFile.path());
    assertThat(generatedCode).contains("      out.write(\"<html>\\n<body>\\n<h2>Hello World!</h2>\\n</body>\\n</html>\");");
  }

  @Test
  void test_with_classpath() throws Exception {
    SensorContextTester ctx = jspContext("<%@ taglib prefix=\"spring\" uri=\"http://www.springframework.org/tags\" %> \n" +
      "<html>\n" +
      "<body>\n" +
      "<h2>Hello World!</h2>\n" +
      "<spring:url value=\"/url/path\" />\n" +
      "</body>\n" +
      "</html>");
    File springJar = Paths.get("target/test-jars/spring-webmvc-5.2.3.RELEASE.jar").toFile();
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, singletonList(springJar));

    assertThat(generatedFiles).hasSize(1);
    InputFile generatedFile = generatedFiles.iterator().next();
    List<String> generatedCode = Files.readAllLines(generatedFile.path());
    assertThat(generatedCode).contains("    org.springframework.web.servlet.tags.UrlTag _jspx_th_spring_005furl_005f0 = new org.springframework.web.servlet.tags.UrlTag();");
  }

  @Test
  void test_compilation_without_webinf() throws Exception {
    SensorContext ctx = jspContext(JSP_SOURCE, tempFolder.resolve("test.jsp"));
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());

    assertThat(generatedFiles).hasSize(1);
    InputFile generatedFile = generatedFiles.iterator().next();
    List<String> generatedCode = Files.readAllLines(generatedFile.path());
    assertThat(generatedCode).contains("      out.write(\"<html>\\n<body>\\n<h2>Hello World!</h2>\\n</body>\\n</html>\");");
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("WEB-INF directory not found, will use basedir as context root");
  }

  @Test
  void test_exception_handling() throws Exception {
    SensorContextTester ctx = jspContext("<%=");
    Collection<GeneratedFile> inputFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(inputFiles).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Error transpiling " + jspFile.toAbsolutePath());
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Some JSP pages failed to transpile. Enable debug log for details.");
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

  @Test
  void test_source_map() throws Exception {
    SensorContextTester ctx = jspContext(JSP_SOURCE);
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(generatedFiles).hasSize(1);
    GeneratedFile generatedFile = generatedFiles.iterator().next();
    assertThat(generatedFile.sourceMap()).isNotNull();
  }

  @Test
  void should_log_warning_when_jasper_fails() throws Exception {
    SensorContextTester ctx = jspContext(JSP_SOURCE);
    Jasper jasper = spy(new Jasper());
    // we make Jasper#getJasperOptions blowup
    doThrow(new IllegalStateException()).when(jasper).getJasperOptions(any(), any());
    Collection<GeneratedFile> generatedFiles = jasper.generateFiles(ctx, emptyList());
    assertThat(generatedFiles).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Failed to transpile JSP files.");
  }

  /**
   * Following test tests execution of Jasper in directory which is a symlink. This was an issue in
   * rev. 24936c9eed88b9886cea36246aae32f6432d2cc9 , but was fixed later on by explicitly setting the context
   * directory instead of relying on automatic lookup.
   */
  @Test
  void test_jasper_with_symlink() throws Exception {
    Path output = tempFolder.resolve("out").toAbsolutePath();
    Files.createDirectories(output);
    Path link = tempFolder.resolve("link");
    Files.createSymbolicLink(link, output);

    Path path = link.resolve("WEB-INF/test.jsp");
    SensorContextTester ctx = jspContext(JSP_SOURCE, path);
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());

    assertThat(generatedFiles).hasSize(1);
    InputFile generatedFile = generatedFiles.iterator().next();
    List<String> generatedCode = Files.readAllLines(generatedFile.path());
    assertThat(generatedCode).contains("      out.write(\"<html>\\n<body>\\n<h2>Hello World!</h2>\\n</body>\\n</html>\");");

  }

  private SensorContextTester jspContext(String jspSource) throws IOException {
    return jspContext(jspSource, webInf.resolve("jsp/test.jsp"));
  }

  private SensorContextTester jspContext(String jspSource, Path path) throws IOException {
    jspFile = createJspFile(jspSource, path);
    SensorContextTester ctx = SensorContextTester.create(tempFolder);
    DefaultInputFile inputFile = TestInputFileBuilder.create("", tempFolder.toFile(), jspFile.toFile())
      .setLanguage("jsp")
      .setContents(jspSource)
      .build();
    ctx.fileSystem().add(inputFile);
    ctx.fileSystem().setWorkDir(workDir);
    return ctx;
  }

  private Path createJspFile(String content, Path path) throws IOException {
    Files.createDirectories(path.getParent());
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    return path;
  }

}
