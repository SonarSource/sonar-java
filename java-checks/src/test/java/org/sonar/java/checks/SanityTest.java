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
package org.sonar.java.checks;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.verifier.FilesUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaCheck;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class SanityTest {

  private static final Logger LOG = Loggers.get(SanityTest.class);

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private static final String TARGET_CLASSES = "target/test-classes";
  private static final String TEST_FILES_EXTRA_CLASSES = "src/test/resources/";

  private static final String[] FILE_DIRECTORIES = {
    mainCodeSourcesPath(""),
    nonCompilingTestSourcesPath(""),
    testCodeSourcesPath(""),
    "src/test/files"
  };

  /**
   * Ignore template rules, as they requires configuration
   */
  private static final Set<String> TEMPLATE_RULES_KEYS = new HashSet<>(Arrays.asList(
    "S124", // CommentRegularExpressionCheck
    "S2253", // DisallowedMethodCheck
    "S3417", // DisallowedDependenciesCheck - XML
    "S3688", // DisallowedClassCheck
    "S3546", // CustomUnclosedResourcesCheck - SE-based
    "S4011" // DisallowedConstructorCheck
  ));

  private static final Set<String> EXCLUDED_RULES = Collections.singleton(
    "S4032" // UselessPackageInfoCheck - which does some file-based tests (parent)
  );

  /**
   * Verifies that playing all the checks on all the test files does not trigger any exceptions.
   *
   * This relies on the fact that a lot of tricky cases are covered in unit tests, on a rule by rule and case by case basis.
   * It does not prevent other rules to fail if similar construct of the language, but not yet encountered.
   */
  @Test
  @EnabledIfSystemProperty(named = "force.sanity.test", matches = "true")
  void test() throws Exception {
    logTester.setLevel(LoggerLevel.WARN);

    List<JavaCheck> checks = getJavaCheckInstances();
    assertThat(checks).isNotEmpty();

    File moduleBaseDir = new File(".").getCanonicalFile().getParentFile();
    List<InputFile> inputFiles = getJavaInputFiles(moduleBaseDir);
    assertThat(inputFiles).hasSizeGreaterThanOrEqualTo(checks.size());

    List<File> classpath = getClassPath();
    assertThat(classpath).isNotEmpty();

    List<SanityCheckException> exceptions = scanFiles(moduleBaseDir, inputFiles, checks, classpath);
    if (!exceptions.isEmpty()) {
      LOG.error(processExceptions(exceptions));
      fail(String.format("Should have been able to execute all the rules on all the test files. %d file(s) made at least 1 rule fail.", exceptions.size()));
    }

    List<LogAndArguments> errorLogs = logTester.getLogs(LoggerLevel.ERROR);
    List<LogAndArguments> parsingErrors = errorLogs.stream()
      .filter(SanityTest::isParseError)
      .collect(Collectors.toList());
    List<LogAndArguments> typeResolutionErrors = errorLogs.stream()
      .filter(SanityTest::isTypeResolutionError)
      .collect(Collectors.toList());

    assertThat(errorLogs).hasSize(24);

    List<LogAndArguments> remainingErrors = new ArrayList<>(errorLogs);
    remainingErrors.removeAll(parsingErrors);
    remainingErrors.removeAll(typeResolutionErrors);
    assertThat(remainingErrors).isEmpty();

    assertThat(typeResolutionErrors)
      .hasSize(16)
      .map(LogAndArguments::getFormattedMsg)
      .map(log -> log.substring("ECJ Unable to resolve type ".length()))
      // FIXME investigate root cause (seems to be a conflict of version, with classes from JDK not resolved correctly
      .containsOnly(
        "javax.servlet.http.Cookie",
        "javax.validation.ConstraintValidator",
        "javax.ws.rs.core.Cookie",
        "javax.ws.rs.core.NewCookie",
        "junit.framework.TestCase",
        "org.apache.commons.lang.math.RandomUtils",
        "org.apache.commons.lang.RandomStringUtils",
        "org.apache.commons.lang3.RandomStringUtils",
        "org.apache.commons.lang3.RandomUtils",
        "org.apache.shiro.web.servlet.SimpleCookie",
        "org.apache.struts.action.Action",
        "org.assertj.core.api.AbstractAssert",
        "org.fest.assertions.GenericAssert",
        "org.springframework.security.web.savedrequest.SavedCookie",
        "play.mvc.Http$Cookie",
        "play.mvc.Http$CookieBuilder");

    assertThat(parsingErrors)
      .hasSize(8)
      .map(LogAndArguments::getFormattedMsg)
      .allMatch(log ->
      // ECJ error message
      log.startsWith("Parse error at line ")
        // analyzer error message mentioning the file
        || log.contains("KeywordAsIdentifierCheck")
        || log.contains("EmptyStatementsInImportsBug")
        || log.contains("RestrictedIdentifiersUsageCheck"));

  }

  private static boolean isParseError(LogAndArguments log) {
    String message = log.getFormattedMsg();
    return message.startsWith("Unable to parse source file : '") || message.startsWith("Parse error at line ");
  }

  private static boolean isTypeResolutionError(LogAndArguments log) {
    return log.getFormattedMsg().startsWith("ECJ Unable to resolve type ");
  }

  private static String processExceptions(List<SanityCheckException> exceptions) {
    StringBuilder sb = new StringBuilder();
    sb.append("At least one rule failed for the following ").append(exceptions.size()).append(" file(s):\n\n");

    int i = 1;
    for (SanityCheckException sanityCheckException : exceptions) {
      sb.append(i).append(")")
        .append("\t").append("File: \"").append(sanityCheckException.inputFile).append("\"\n")
        .append("\t").append("Rule: \"").append(sanityCheckException.failingCheck).append("\"\n")
        .append("\t").append("Exception: \"").append(sanityCheckException.realCause).append("\"\n")
        .append("\n");
      i++;
    }

    return sb.toString();
  }

  private static List<JavaCheck> getJavaCheckInstances() {
    List<Class<? extends JavaCheck>> checkClasses = new ArrayList<>();
    checkClasses.addAll(CheckList.getJavaChecks());
    checkClasses.addAll(CheckList.getJavaTestChecks());

    List<JavaCheck> javaChecks = new ArrayList<>();
    for (Class<? extends JavaCheck> checkClass : checkClasses) {
      String ruleKey = AnnotationUtils.getAnnotation(checkClass, org.sonar.check.Rule.class).key();
      if (TEMPLATE_RULES_KEYS.contains(ruleKey) || EXCLUDED_RULES.contains(ruleKey)) {
        // FIXME initialize a new template rule with some default parameter ?!
      } else {
        try {
          javaChecks.add(checkClass.getConstructor().newInstance());
        } catch (Exception e) {
          fail(String.format("Unable to initialize rule %s", ruleKey), e);
        }
      }
    }
    return javaChecks;
  }

  private static List<InputFile> getJavaInputFiles(File moduleBaseDir) {
    return Stream.of(FILE_DIRECTORIES)
      .map(File::new)
      .map(File::toPath)
      .map(p -> FilesUtils.getFilesRecursively(p, "java"))
      .flatMap(List::stream)
      .map(File::getAbsolutePath)
      .filter(SanityTest::isNotParsingErrorFile)
      .map(path -> inputFile(moduleBaseDir, path))
      .collect(Collectors.toList());
  }

  private static boolean isNotParsingErrorFile(String filename) {
    return !(filename.contains("ParsingError") || filename.contains("ParseError"));
  }

  private static List<File> getClassPath() {
    List<File> classpath = new ArrayList<>();
    classpath.addAll(FilesUtils.getClassPath(FilesUtils.DEFAULT_TEST_JARS_DIRECTORY));
    classpath.add(new File(TARGET_CLASSES));
    classpath.add(new File(TEST_FILES_EXTRA_CLASSES));
    return classpath;
  }

  private static List<SanityCheckException> scanFiles(File moduleBaseDir, List<InputFile> inputFiles, List<JavaCheck> checks, List<File> classpath) {
    List<SanityCheckException> exceptions = new ArrayList<>();
    SonarComponents sonarComponents = sonarComponents(moduleBaseDir, inputFiles);
    for (InputFile inputFile : inputFiles) {
      try {
        VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(checks, classpath, sonarComponents, new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED));
        JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge, null);
      } catch (Throwable e) {
        exceptions.add(new SanityCheckException(inputFile, e));
      }
    }
    return exceptions;
  }

  private static class SanityCheckException extends Exception {
    private static final long serialVersionUID = 1934785706394840975L;
    private final InputFile inputFile;
    @Nullable
    private final Throwable realCause;
    @Nullable
    private final String failingCheck;

    public SanityCheckException(InputFile inputFile, Throwable analysisException) {
      super(String.format("Unable to analyse file %s", inputFile.filename()), analysisException);
      this.inputFile = inputFile;
      this.realCause = analysisException.getCause();
      this.failingCheck = parseStackForCheck(realCause);
    }

    @Nullable
    private static String parseStackForCheck(@Nullable Throwable realCause) {
      if (realCause == null) {
        return null;
      }
      String stackTrace = ExceptionUtils.getStackTrace(realCause);
      return Arrays.stream(stackTrace.split("\n"))
        // try to retrieve the rule class name in 'checks' package
        .filter(line -> line.contains("org.sonar.java.checks."))
        // check that it's a rule
        .filter(line -> line.substring(0, line.lastIndexOf(".java:")).endsWith("Check"))
        .findFirst()
        // drop parenthesis to get location
        .map(line -> line.substring(line.lastIndexOf('(') + 1, line.length() - 1))
        .orElse(null);
    }
  }

  private static SonarComponents sonarComponents(File moduleBaseDir, List<InputFile> inputFiles) {
    SensorContextTester context = SensorContextTester.create(moduleBaseDir).setRuntime(SonarRuntimeImpl.forSonarLint(Version.create(6, 7)));
    context.setSettings(new MapSettings()
      .setProperty(SonarComponents.FAIL_ON_EXCEPTION_KEY, true)
      .setProperty(SonarComponents.SONAR_BATCH_MODE_KEY, true));
    DefaultFileSystem fileSystem = context.fileSystem();
    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, null, null, null) {
      @Override
      public boolean reportAnalysisError(RecognitionException re, InputFile inputFile) {
        return false;
      }
    };
    sonarComponents.setSensorContext(context);
    inputFiles.forEach(inputFile -> fileSystem.add(inputFile));
    return sonarComponents;
  }

  private static DefaultInputFile inputFile(File moduleBaseDir, String filename) {
    File file = new File(filename);
    try {
      return new TestInputFileBuilder("", moduleBaseDir, file)
        .setContents(new String(Files.readAllBytes(file.toPath()), UTF_8))
        .setCharset(UTF_8)
        .setLanguage("java")
        .build();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to lead file '%s", file.getAbsolutePath()));
    }
  }
}
