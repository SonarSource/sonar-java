/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.plugins.java;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.Version;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.verifier.FilesUtils;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPathInModule;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;
import static org.sonar.java.test.classpath.TestClasspathUtils.AWS_MODULE;
import static org.sonar.java.test.classpath.TestClasspathUtils.DEFAULT_MODULE;

class SanityTest {

  private static final Logger LOG = LoggerFactory.getLogger(SanityTest.class);

  @RegisterExtension
  public final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private static final String[] COMPILING_FILE_DIRECTORIES = {
    mainCodeSourcesPath(""),
    testCodeSourcesPath(""),
    mainCodeSourcesPathInModule(AWS_MODULE, "")
  };

  private static final String[] NON_COMPILING_FILE_DIRECTORIES = {
    nonCompilingTestSourcesPath(""),
    nonCompilingTestSourcesPathInModule(AWS_MODULE, ""),
    "../java-checks/src/test/files"
  };

  /**
   * Ignore template rules, as they requires configuration
   */
  private static final Set<String> TEMPLATE_RULES_KEYS = new HashSet<>(Arrays.asList(
    "S124", // CommentRegularExpressionCheck
    "S2253", // DisallowedMethodCheck
    "S3417", // DisallowedDependenciesCheck - XML
    "S3688", // DisallowedClassCheck
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
  void scan_non_compiling_files() throws Exception {
    logTester.setLevel(Level.WARN);

    List<JavaCheck> checks = getJavaCheckInstances();

    File moduleBaseDir = new File(".").getCanonicalFile().getParentFile();
    List<InputFile> inputFiles = getJavaInputFiles(moduleBaseDir, NON_COMPILING_FILE_DIRECTORIES);
    assertThat(inputFiles).hasSizeGreaterThan(350);

    List<File> classpathList = getClasspath();
    List<SanityCheckException> exceptions = scanFiles(moduleBaseDir, inputFiles, checks, classpathList);
    if (!exceptions.isEmpty()) {
      LOG.error(processExceptions(exceptions));
      fail(String.format("Should have been able to execute all the rules on all the test files. %d file(s) made at least 1 rule fail.", exceptions.size()));
    }

    List<LogAndArguments> errorLogs = logTester.getLogs(Level.ERROR);

    assertThat(errorLogs)
      .hasSize(4)
      .allMatch(SanityTest::isParseError)
      .map(LogAndArguments::getFormattedMsg)
      .allMatch(log ->
      // ECJ error message
      log.startsWith("Parse error at line ")
        // analyzer error message mentioning the file
        || log.contains("RestrictedIdentifiersUsageCheckSample")
        || log.contains("EmptyStatementsInImportsBug"));
  }

  @Test
  @EnabledIfSystemProperty(named = "force.sanity.test", matches = "true")
  void scan_compiling_files() throws Exception {
    logTester.setLevel(Level.WARN);

    List<JavaCheck> checks = getJavaCheckInstances();

    File moduleBaseDir = new File(".").getCanonicalFile().getParentFile();
    List<InputFile> inputFiles = getJavaInputFiles(moduleBaseDir, COMPILING_FILE_DIRECTORIES);
    assertThat(inputFiles).hasSizeGreaterThan(400);

    List<File> classpath = getClasspath();

    List<SanityCheckException> exceptions = scanFiles(moduleBaseDir, inputFiles, checks, classpath);
    if (!exceptions.isEmpty()) {
      LOG.error(processExceptions(exceptions));
      fail(String.format("Should have been able to execute all the rules on all the test files. %d file(s) made at least 1 rule fail.", exceptions.size()));
    }
    assertThat(logTester.getLogs(Level.ERROR)).isEmpty();
  }

  private static List<File> getClasspath() {
    Set<File> classpathSet = new LinkedHashSet<>();
    classpathSet.addAll(DEFAULT_MODULE.getClassPath());
    classpathSet.addAll(AWS_MODULE.getClassPath());
    assertThat(classpathSet).isNotEmpty();
    return classpathSet.stream().toList();
  }

  private static boolean isParseError(LogAndArguments log) {
    String message = log.getFormattedMsg();
    return message.startsWith("Unable to parse source file : '") || message.startsWith("Parse error at line ");
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
    checkClasses.addAll(GeneratedCheckList.getJavaChecks());
    checkClasses.addAll(GeneratedCheckList.getJavaTestChecks());

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
    assertThat(javaChecks).hasSizeGreaterThan(400);
    return javaChecks;
  }

  private static List<InputFile> getJavaInputFiles(File moduleBaseDir, String[] directories) {
    return Stream.of(directories)
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

  private static List<SanityCheckException> scanFiles(File moduleBaseDir, List<InputFile> inputFiles, List<JavaCheck> checks, List<File> classpath) {
    SonarComponents sonarComponents = sonarComponents(moduleBaseDir, inputFiles);
    JavaAstScanner scanner = new JavaAstScanner(sonarComponents, new NoOpTelemetry(), TelemetryKey.JAVA_ANALYSIS_MAIN);
    JavaVersion javaVersion = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    scanner.setVisitorBridge(new VisitorsBridgeForTests.Builder(checks)
      .enableSemanticWithProjectClasspath(classpath)
      .withSonarComponents(sonarComponents)
      .withJavaVersion(javaVersion)
      .build());
    List<SanityCheckException> exceptions = new ArrayList<>();
    AnalysisProgress analysisProgress = new AnalysisProgress(inputFiles.size());
    JParserConfig.Mode.BATCH
      .create(javaVersion, classpath, true)
      .parse(inputFiles, () -> false, analysisProgress, (input, result) -> {
        try {
          scanner.simpleScan(input, result, ast -> {});
        } catch (Throwable e) {
          exceptions.add(new SanityCheckException(input, e));
        }
      });
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
    DefaultFileSystem fileSystem = context.fileSystem();
    SonarComponents sonarComponents = new SonarComponents(null, fileSystem, null, null, null, null) {
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
