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
package org.sonar.java.jsp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.model.GeneratedFile;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class JasperTest {

  private static final String JSP_SOURCE = "<html>\n" +
    "<body>\n" +
    "<h2>Hello World!</h2>\n" +
    "</body>\n" +
    "</html>";

  private static final String SPRING_TLD = "<%@ taglib prefix=\"spring\" uri=\"http://www.springframework.org/tags\" %>\n";

  Path tempFolder;
  Path webInf;

  @TempDir
  Path workDir;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);
  private Path jspFile;
  private final File springJar = Paths.get("target/test-jars/spring-webmvc-5.2.3.RELEASE.jar").toFile();
  private final File jstlJar = Paths.get("target/test-jars/jstl-1.2.jar").toFile();
  private final File jee6Jar = Paths.get("target/test-jars/javaee-web-api-6.0.jar").toFile();

  @BeforeEach
  void setUp() throws Exception {
    // on macOS tmp is symbolic link which doesn't work well with Jasper, so we create tmp in 'target/tmp'
    tempFolder = Paths.get("target/tmp");
    webInf = tempFolder.resolve("src/main/webapp/WEB-INF");
    Files.createDirectories(webInf);
  }

  @AfterEach
  void tearDown() throws Exception {
    PathUtils.delete(tempFolder);
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
  void test_exclude_unrelated_files() throws Exception {
    SensorContextTester ctx = jspContext(JSP_SOURCE);
    ctx.setSettings(new MapSettings().setProperty("sonar.exclusions", "**/*something.xml"));
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(generatedFiles).hasSize(1);
  }

  @Test
  void test_exclude_all_jsp() throws Exception {
    SensorContextTester ctx = jspContext(JSP_SOURCE);
    ctx.setSettings(new MapSettings().setProperty("sonar.exclusions", "**/*_jsp.java"));
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(generatedFiles).isEmpty();
  }

  @Test
  void test_exclude_current_jsp() throws Exception {
    SensorContextTester ctx = jspContext(JSP_SOURCE);
    ctx.setSettings(new MapSettings().setProperty("sonar.exclusions", "**/any.js,**/test_jsp.java"));
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(generatedFiles).isEmpty();
  }

  @Test
  void test_exclude_filter() throws Exception {
    List<String> sonarExclusions = List.of("**/*A_jsp.java", "**\\*B_jsp.java", " *C_jsp.java ");
    Predicate<String> filter = Jasper.createExclusionFilter(sonarExclusions);
    assertThat(filter.test(null)).isTrue();
    assertThat(filter.test("X_jsp.java")).isFalse();
    assertThat(filter.test("A_jsp.java")).isTrue();
    assertThat(filter.test("B_jsp.java")).isTrue();
    assertThat(filter.test("C_jsp.java")).isTrue();
    assertThat(filter.test("folder/A_jsp.java")).isTrue();
    assertThat(filter.test("folder\\A_jsp.java")).isTrue();
    assertThat(filter.test("folder/B_jsp.java")).isTrue();
    assertThat(filter.test("folder\\B_jsp.java")).isTrue();
    assertThat(filter.test("folder/B_JSP.JAVA")).isFalse();
    assertThat(filter.test("folder/C_jsp.java")).isFalse();
    assertThat(filter.test("A.java")).isFalse();
  }

  @Test
  void test_with_classpath() throws Exception {
    SensorContextTester ctx = jspContext(SPRING_TLD +
      "<html>\n" +
      "<body>\n" +
      "<h2>Hello World!</h2>\n" +
      "<spring:url value=\"/url/path\" />\n" +
      "</body>\n" +
      "</html>");
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, singletonList(springJar));

    assertThat(generatedFiles).hasSize(1);
    InputFile generatedFile = generatedFiles.iterator().next();
    List<String> generatedCode = Files.readAllLines(generatedFile.path());
    assertThat(generatedCode).contains("    org.springframework.web.servlet.tags.UrlTag _jspx_th_spring_005furl_005f0 = new org.springframework.web.servlet.tags.UrlTag();");
  }


  @Test
  void test_with_classpath_jee6_jstl() throws Exception {
    SensorContextTester ctx = jspContext(
      "<%@ taglib uri = \"http://java.sun.com/jsp/jstl/core\" prefix = \"c\" %>\n" +
      "<html>\n" +
      "<body>\n" +
      "<h2>Hello World!</h2>\n" +
      "<c:if test=\"true\">what-if</c:if>\n" +
      "</body>\n" +
      "</html>");
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, asList(jee6Jar, jstlJar));

    assertThat(generatedFiles).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).matches(logs -> logs.stream().anyMatch(line ->
      line.startsWith("Error transpiling src/main/webapp/WEB-INF/jsp/test.jsp. Error:\njava.lang.ClassFormatError")));
  }

  @Test
  void test_compilation_without_webinf() throws Exception {
    SensorContext ctx = jspContext(JSP_SOURCE, tempFolder.resolve("test.jsp"));
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList());

    assertThat(generatedFiles).hasSize(1);
    InputFile generatedFile = generatedFiles.iterator().next();
    List<String> generatedCode = Files.readAllLines(generatedFile.path());
    assertThat(generatedCode).contains("      out.write(\"<html>\\n<body>\\n<h2>Hello World!</h2>\\n</body>\\n</html>\");");
    assertThat(logTester.logs(Level.DEBUG)).contains("WEB-INF directory not found, will use basedir as context root");
  }

  @Test
  void test_exception_handling() throws Exception {
    SensorContextTester ctx = jspContext("<%=");
    Collection<GeneratedFile> inputFiles = new Jasper().generateFiles(ctx, emptyList());
    assertThat(inputFiles).isEmpty();
    assertThat(logTester.logs(Level.DEBUG))
      .matches(logs -> logs.stream().anyMatch(line ->
        line.startsWith("Error transpiling src/main/webapp/WEB-INF/jsp/test.jsp.")));
    assertThat(logTester.logs(Level.WARN)).contains("Some JSP pages failed to transpile. Enable debug log for details.");
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
    assertThat(logTester.logs(Level.WARN)).contains("Failed to transpile JSP files.");
  }

  /**
   * Following test tests execution of Jasper in directory which is a symlink. This was an issue in
   * rev. 24936c9eed88b9886cea36246aae32f6432d2cc9 , but was fixed later on by explicitly setting the context
   * directory instead of relying on automatic lookup.
   *
   * This test might fail on Windows when run with the non-administrator account due to JDK issue
   * https://bugs.openjdk.java.net/browse/JDK-8218418, it was fixed in JDK 13
   */
  @DisabledOnOs(WINDOWS)
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

  @Test
  void test_compile_custom_tag() throws Exception {
    String tagLib = "<%@ taglib prefix=\"t\" tagdir=\"/WEB-INF/tags\" %>";
    SensorContextTester ctx = jspContext(tagLib +
      "<t:mytag />");
    // spring tag library is used to complete coverage in Jasper classloader - it has resource which is on the project's
    // classpath
    createJspFile(tagLib +
      SPRING_TLD +
      "<spring:url value=\"/url/path\" />\n" +
      "<h2>Hello World!</h2>", webInf.resolve("tags/mytag.tag"));
    Collection<GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, singletonList(springJar));

    assertThat(generatedFiles).hasSize(1);
    GeneratedFile testJspFile = generatedFiles.iterator().next();
    List<String> testJsp = Files.readAllLines(testJspFile.path());
    assertThat(testJsp).contains(
      "    org.apache.jsp.tag.web.mytag_tag _jspx_th_t_005fmytag_005f0 = new org.apache.jsp.tag.web.mytag_tag();");
  }

  @Test
  void test_failing_tag_compilation() throws Exception {
    String tagLib = "<%@ taglib prefix=\"t\" tagdir=\"/WEB-INF/tags\" %>";
    SensorContextTester ctx = jspContext(tagLib +
      "<t:mytag />");
    createJspFile(tagLib + "<% new Missing(); %> ", webInf.resolve("tags/mytag.tag"));
    Map<String, GeneratedFile> generatedFiles = new Jasper().generateFiles(ctx, emptyList()).
      stream().collect(Collectors.toMap(GeneratedFile::filename, f -> f));

    assertThat(generatedFiles).isEmpty();
    assertThat(logTester.logs(Level.DEBUG))
      .matches(logs -> logs.stream().anyMatch(line ->
      line.startsWith("Error transpiling src/main/webapp/WEB-INF/jsp/test.jsp. Error:\norg.apache.jasper.JasperException:")));

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
