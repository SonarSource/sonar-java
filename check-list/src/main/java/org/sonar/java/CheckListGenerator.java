/*
 * SonarQube Java
 * Copyright (C) 2024-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;

public class CheckListGenerator {
  private static final String CLASS_NAME = "GeneratedCheckList";
  public static final String RULES_PATH = "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/";
  private final Gson gson;

  final Path relativePath;
  final Path awsRelativePath;
  Path pathToWriteList;
  final String rulesPath;

  public CheckListGenerator(Gson gson, Path relativePath, Path awsRelativePath, Path pathToWriteList, String rulesPath) {
    this.gson = gson;
    this.relativePath = relativePath;
    this.awsRelativePath = awsRelativePath;
    this.pathToWriteList = pathToWriteList;
    this.rulesPath = rulesPath;
  }

  public static void main(String[] args) {
    var generator = new CheckListGenerator(new Gson(),
      Path.of("java-checks/src/main/java"),
      Path.of("java-checks-aws/src/main/java"),
      Path.of("check-list/target/generated-sources/" + CLASS_NAME + ".java"),
      RULES_PATH);
    generator.generateCheckList();
  }

  public void generateCheckList() {
    var checks = getCheckClasses();

    List<Class<?>> mainClasses = new ArrayList<>();
    List<Class<?>> testClasses = new ArrayList<>();
    List<Class<?>> allClasses = new ArrayList<>();
    generateCheckListClasses(checks, mainClasses, testClasses, allClasses, rulesPath);
    String main = collectChecks(mainClasses);
    String test = collectChecks(testClasses);
    String all = collectChecks(allClasses);

    String importChecks = generateImportStatements(checks);

    writeToFile(importChecks, main, test, all, pathToWriteList);
  }

  public List<Class<?>> getCheckClasses() {
    var checkFiles = getCheckFiles();
    return checkFiles.stream()
      .<Class<?>>map(CheckListGenerator::getClassByName)
      .filter(c -> !Modifier.isAbstract(c.getModifiers()))
      .filter(c -> c.getAnnotationsByType(Rule.class).length != 0)
      .toList();
  }

  private List<String> getCheckFiles() {
    try (Stream<Path> stream = Stream.concat(Files.walk(relativePath.resolve("org/sonar/java/checks")), Files.walk(awsRelativePath.resolve("org/sonar/java/checks")))) {
      return stream
        .map(p -> p.startsWith(awsRelativePath) ? awsRelativePath.relativize(p).toString() : relativePath.relativize(p).toString())
        .filter(file -> file.endsWith("Check.java"))
        .map(file -> file.replace(".java", "").replace(File.separator, "."))
        .sorted()
        .toList();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  private static Class<?> getClassByName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Cannot find the class for name " + className, e);
    }
  }

  public String getRuleKey(Class<?> check) {
    return check.getAnnotation(Rule.class).key();
  }

  protected Metadata getMetadata(Reader reader) {
    return gson.fromJson(reader, Metadata.class);
  }

  public static String generateImportStatements(List<Class<?>> checks) {
    return checks.stream()
      .map(c -> "import " + c.getPackageName() + "." + c.getSimpleName() + ";")
      .collect(Collectors.joining("\n"));
  }

  public String collectChecks(List<Class<?>> classes) {
    return classes.stream()
      .map(c -> c.getSimpleName() + ".class")
      .collect(Collectors.joining(", \n    "));
  }

  public void generateCheckListClasses(List<Class<?>> checks, List<Class<?>> mainClasses, List<Class<?>> testClasses, List<Class<?>> allClasses, String rulesPath) {
    checks.forEach(check -> {
      String ruleKey = getRuleKey(check);
      String fileName = rulesPath + ruleKey + ".json";
      try (BufferedReader reader = Files.newBufferedReader(Path.of(fileName), StandardCharsets.UTF_8)) {
        Metadata metadata = getMetadata(reader);
        switch (metadata.scope) {
          case "All" -> allClasses.add(check);
          case "Main" -> mainClasses.add(check);
          case "Tests" -> testClasses.add(check);
          default -> throw new IllegalStateException("Unknown scope " + metadata.scope + " for class " + check.getName());
        }
      } catch (IOException e) {
        throw new IllegalStateException("Could not find rule file " + fileName, e);
      }
    });
  }

  public void writeToFile(String importChecks, String mainChecks, String testChecks, String allChecks, Path path) {
    String content = """
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
          ${mainChecks});

        private static final List<Class<? extends JavaCheck>> JAVA_TEST_CHECKS = Arrays.asList(
          ${testChecks});

        private static final List<Class<? extends JavaCheck>> JAVA_MAIN_AND_TEST_CHECKS = Arrays.asList(
          ${allChecks});

        private static final List<Class<?>> ALL_CHECKS = Stream.of(JAVA_MAIN_CHECKS, JAVA_MAIN_AND_TEST_CHECKS, JAVA_TEST_CHECKS)
          .flatMap(List::stream)
          .sorted(Comparator.comparing(Class::getSimpleName))
          .collect(Collectors.toList());

          private static final Set<Class<? extends JavaCheck>> JAVA_CHECKS_NOT_WORKING_FOR_AUTOSCAN = Set.of(
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
            EqualsNotOverriddenWithCompareToCheck.class,
            EqualsOverriddenWithHashCodeCheck.class,
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
      .replace("${mainChecks}", mainChecks)
      .replace("${testChecks}", testChecks)
      .replace("${allChecks}", allChecks);

    try {
      Files.writeString(path, content);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write checks to the file.", e);
    }
  }

  protected static class Metadata {
    String scope;
  }
}
