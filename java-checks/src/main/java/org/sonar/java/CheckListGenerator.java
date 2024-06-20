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
package org.sonar.java;

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
import org.sonar.check.Rule;

public class CheckListGenerator {

  private static final String CLASS_NAME = "GeneratedCheckList";

  public static void main(String[] args) {
    var checks = getCheckClasses();

    List<Class<?>> mainClasses = new ArrayList<>();
    List<Class<?>> testClasses = new ArrayList<>();
    List<Class<?>> allClasses = new ArrayList<>();

    checks.forEach(check -> {
      var ruleKey = check.getAnnotation(Rule.class).key();
      var fileName = "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/" + ruleKey + ".json";
      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException("Could not find rule file " + fileName, e);
      }
      var metadata = new Gson().fromJson(br, Metadata.class);

      switch (metadata.scope) {
        case "All" -> allClasses.add(check);
        case "Main" -> mainClasses.add(check);
        case "Tests" -> testClasses.add(check);
        default -> throw new IllegalStateException("Unknown scope " + metadata.scope + " for class " + check.getName());
      }
    });

    var importChecks = checks.stream()
      .map(c -> c.getPackageName() + "." + c.getSimpleName())
      .map(c -> "import " + c + ";")
      .collect(Collectors.joining("\n"));

    try {
      writeToFile(importChecks, collectChecks(mainClasses), collectChecks(testClasses), collectChecks(allClasses));
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write checks to the file.", e);
    }
  }

  private static List<? extends Class<?>> getCheckClasses() {
    List<String> modifiedFiles;
    var relativePath = Path.of("java-checks/src/main/java");
    try (var stream = Files.walk(relativePath.resolve("org/sonar/java/checks"))) {
      modifiedFiles = stream.map(p -> relativePath.relativize(p).toString())
        .filter(file -> file.endsWith("Check.java"))
        .map(file -> file.replace(".java", ""))
        .map(file -> file.replace(File.separator, "."))
        .toList();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    var classes = modifiedFiles.stream()
      .map(file -> {
        try {
          return Class.forName(file);
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException("Can not find the class for name " + file, e);
        }
      })
      .toList();

    return classes.stream()
      .filter(c -> !Modifier.isAbstract(c.getModifiers()))
      .filter(c -> c.getAnnotationsByType(Rule.class).length != 0)
      .toList();
  }

  private static String collectChecks(List<Class<?>> allClasses) {
    return allClasses.stream()
      .map(c -> c.getSimpleName() + ".class")
      .collect(Collectors.joining(", \n    "));
  }

  private static void writeToFile(String importChecks, String collectMainChecks, String collectTestChecks, String collectAllChecks) throws IOException {
    try {
      var path = Path.of("java-checks/target/generated-sources/" + CLASS_NAME + ".java");
      Files.writeString(path, """
        package org.sonar.java;

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

            private static final Set<Class<? extends JavaCheck>> JAVA_CHECKS_NOT_WORKING_FOR_AUTOSCAN = Set.of(
              // Symbolic executions rules are not in this list because they are dynamically excluded
              // Rules relying on correct setup of jdk.home
              CallToDeprecatedCodeMarkedForRemovalCheck.class,
              CallToDeprecatedMethodCheck.class,
              // Rules relying on correct setup of java version
              AbstractClassNoFieldShouldBeInterfaceCheck.class,
              AnonymousClassShouldBeLambdaCheck.class,
              CombineCatchCheck.class,
              DateAndTimesCheck.class,
              DateUtilsTruncateCheck.class,
              DiamondOperatorCheck.class,
              InsecureCreateTempFileCheck.class,
              JdbcDriverExplicitLoadingCheck.class,
              LambdaOptionalParenthesisCheck.class,
              LambdaSingleExpressionCheck.class,
              RepeatAnnotationCheck.class,
              ReplaceGuavaWithJavaCheck.class,
              ReplaceLambdaByMethodRefCheck.class,
              SwitchInsteadOfIfSequenceCheck.class,
              ThreadLocalWithInitialCheck.class,
              TryWithResourcesCheck.class,
              ValueBasedObjectUsedForLockCheck.class,
              // Rules with a high deviation (>3%)
              AccessibilityChangeCheck.class,
              CipherBlockChainingCheck.class,
              ClassNamedLikeExceptionCheck.class,
              ClassWithOnlyStaticMethodsInstantiationCheck.class,
              CollectionInappropriateCallsCheck.class,
              DeadStoreCheck.class,
              EqualsArgumentTypeCheck.class,
              EqualsNotOverridenWithCompareToCheck.class,
              EqualsOverridenWithHashCodeCheck.class,
              ForLoopVariableTypeCheck.class,
              JWTWithStrongCipherCheck.class,
              MethodNamedEqualsCheck.class,
              NioFileDeleteCheck.class,
              PrivateFieldUsedLocallyCheck.class,
              SillyEqualsCheck.class,
              StandardCharsetsConstantsCheck.class,
              ThreadLocalCleanupCheck.class,
              ThreadOverridesRunCheck.class,
              UnusedPrivateClassCheck.class,
              UnusedPrivateFieldCheck.class,
              VerifiedServerHostnamesCheck.class,
              VolatileNonPrimitiveFieldCheck.class,
              WeakSSLContextCheck.class);

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

  private static class Metadata {
    String scope;
  }

}