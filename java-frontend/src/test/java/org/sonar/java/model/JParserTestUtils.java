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
package org.sonar.java.model;

import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

public class JParserTestUtils {

  private JParserTestUtils() {
    // Utility class
  }

  public static final List<File> DEFAULT_CLASSPATH = Arrays.asList(new File("target/test-classes"), new File("target/classes"));

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
    return JParser.parse(JParser.MAXIMUM_SUPPORTED_JAVA_VERSION, unitName, source, classpath);
  }

}
