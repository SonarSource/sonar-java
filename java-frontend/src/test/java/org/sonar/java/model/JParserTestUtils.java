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
package org.sonar.java.model;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;

public class JParserTestUtils {

  private JParserTestUtils() {
    // Utility class
  }

  public static final List<File> DEFAULT_CLASSPATH = Arrays.asList(new File("target/test-classes"), new File("target/classes"));

  public static final Path CHECKS_TEST_DIR = Paths.get("..", "java-checks-test-sources", "default");

  public static CompilationUnitTree parse(File file) {
    return parse(file, DEFAULT_CLASSPATH);
  }

  public static CompilationUnitTree parse(File file, List<File> classpath) {
    String source;
    try {
      source = Files.readLines(file, StandardCharsets.UTF_8).stream().collect(Collectors.joining("\n"));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read file", e);
    }
    return parse(file.getName(), source, classpath);
  }

  public static CompilationUnitTree parse(String source) {
    return parse("File.java", source);
  }

  public static CompilationUnitTree parseModule(String... lines) {
    return parse("module-info.java", Arrays.stream(lines).collect(Collectors.joining("\n")));
  }

  public static CompilationUnitTree parsePackage(String... lines) {
    return parse("package-info.java", Arrays.stream(lines).collect(Collectors.joining("\n")));
  }

  private static CompilationUnitTree parse(String unitName, String source) {
    return parse(unitName, source, DEFAULT_CLASSPATH);
  }

  public static CompilationUnitTree parse(String unitName, String source, List<File> classpath) {
    JavaVersion version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    return JParser.parse(JParserConfig.Mode.FILE_BY_FILE.create(version, classpath).astParser(), version.toString(), unitName, source);
  }

  public static List<File> checksTestClassPath() throws IOException {
    Path testProjectDir = CHECKS_TEST_DIR.toRealPath();
    List<File> classPath = new ArrayList<>();
    classPath.add(testProjectDir.resolve(Paths.get("target", "classes")).toFile());
    Path jarFolder = testProjectDir.resolve(Paths.get("target", "test-jars"));
    try (
      Stream<Path> walker = java.nio.file.Files.list(jarFolder)) {
      walker
        .filter(path -> path.getFileName().toString().endsWith(".jar"))
        .map(Path::toFile)
        .forEach(classPath::add);
    }
    assertThat(classPath).hasSizeGreaterThan(300);
    return classPath;
  }

}
