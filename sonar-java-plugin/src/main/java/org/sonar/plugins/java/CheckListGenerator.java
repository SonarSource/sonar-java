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
package org.sonar.plugins.java;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.sonar.check.Rule;

public class CheckListGenerator {

  private static class Metadata {
    String scope;
  }

  private static final String CLASS_NAME = "GeneratedCheckList";

  public static void main(String[] args) throws IOException {

    var filter = FileFilterUtils.suffixFileFilter("Check.java");
    var files = FileUtils.listFiles(new File("java-checks/src/main/java/org/sonar/java/checks"), filter, FileFilterUtils.trueFileFilter());

    var modifiedFiles = files.stream()
      .map(File::toString)
      .map(file -> file.replace("java-checks/src/main/java/", ""))
      .map(file -> file.replace(".java", ""))
      .map(file -> file.replace("/", "."))
      .toList();

    var classes = modifiedFiles.stream()
      .map(file -> {
        try {
          return Class.forName(file);
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException("Can not find the class for name " + file, e);
        }
      })
      .toList();

    var filteredClasses = classes.stream()
      .filter(c -> !Modifier.isAbstract(c.getModifiers()))
      .filter(c -> c.getAnnotationsByType(Rule.class).length != 0)
      .toList();

    List<Class<?>> mainClasses = new ArrayList<>();
    List<Class<?>> testClasses = new ArrayList<>();
    List<Class<?>> allClasses = new ArrayList<>();

    filteredClasses.forEach(c -> {
      Rule ruleAnnotation = c.getAnnotationsByType(Rule.class)[0];
      String key = ruleAnnotation.key();
      String fileName = "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/" + key + ".json";
      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException("Could not find rule file " + fileName, e);
      }
      Metadata metadata = new Gson().fromJson(br, Metadata.class);

      switch (metadata.scope) {
        case "All" -> allClasses.add(c);
        case "Main" -> mainClasses.add(c);
        case "Tests" -> testClasses.add(c);
        default -> throw new IllegalStateException("Unknown scope " + metadata.scope + " for class " + c.getName());
      }
    });

    String importChecks = filteredClasses.stream()
      .map(c -> c.getPackageName() + "." + c.getSimpleName())
      .map(c -> "import " + c + ";")
      .collect(Collectors.joining("\n"));

    String collectMainChecks = mainClasses.stream()
      .map(c -> c.getSimpleName() + ".class")
      .collect(Collectors.joining(", \n    "));

    String collectTestChecks = testClasses.stream()
      .map(c -> c.getSimpleName() + ".class")
      .collect(Collectors.joining(", \n    "));

    String collectAllChecks = allClasses.stream()
      .map(c -> c.getSimpleName() + ".class")
      .collect(Collectors.joining(", \n    "));

    try {
      Path path = Path.of("sonar-java-plugin/target/generated-sources/" + CLASS_NAME + ".java");
      Files.writeString(path, """
        package org.sonar.plugins.java;

        import java.util.Arrays;
        import java.util.Comparator;
        import java.util.List;
        import java.util.Set;
        import java.util.stream.Collectors;
        import java.util.stream.Stream;
        import org.sonar.plugins.java.api.JavaCheck;

        ${importChecks}

        public final class ${className} {

          public static final String REPOSITORY_KEY = "java";

          private static final List<Class<? extends JavaCheck>> JAVA_MAIN_CHECKS = Arrays.asList(
            ${mainClasses});

          private static final List<Class<? extends JavaCheck>> JAVA_TEST_CHECKS = Arrays.asList(
            ${testClasses});

          private static final List<Class<? extends JavaCheck>> JAVA_MAIN_AND_TEST_CHECKS = Arrays.asList(
            ${allClasses});

          private static final List<Class<?>> ALL_CHECKS = Stream.of(JAVA_MAIN_CHECKS, JAVA_MAIN_AND_TEST_CHECKS, JAVA_TEST_CHECKS)
            .flatMap(List::stream)
            .sorted(Comparator.comparing(Class::getSimpleName))
            .collect(Collectors.toList());

          private static final Set<Class<? extends JavaCheck>> JAVA_CHECKS_NOT_WORKING_FOR_AUTOSCAN = Set.of();

          private GeneratedCheckList() {
          }

          public static List<Class<?>> getChecks() {
            return ALL_CHECKS;
          }

          public static List<Class<? extends JavaCheck>> getJavaChecks() {
            return sortedJoin(JAVA_MAIN_CHECKS, JAVA_MAIN_AND_TEST_CHECKS);
          }

          public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
            return sortedJoin(JAVA_MAIN_AND_TEST_CHECKS, JAVA_TEST_CHECKS);
          }

          public static Set<Class<? extends JavaCheck>> getJavaChecksNotWorkingForAutoScan() {
            return JAVA_CHECKS_NOT_WORKING_FOR_AUTOSCAN;
          }

          @SafeVarargs
          private static List<Class<? extends JavaCheck>> sortedJoin(List<Class<? extends JavaCheck>>... lists) {
            return Arrays.stream(lists)
              .flatMap(List::stream)
              .sorted(Comparator.comparing(Class::getSimpleName))
              .toList();
          }
        }
        """
        .replace("${importChecks}", importChecks)
        .replace("${className}", CLASS_NAME)
        .replace("${mainClasses}", collectMainChecks)
        .replace("${testClasses}", collectTestChecks)
        .replace("${allClasses}", collectAllChecks));

    } catch (IOException e) {
      throw new IOException("Failed to generate class", e);
    }
  }

}
