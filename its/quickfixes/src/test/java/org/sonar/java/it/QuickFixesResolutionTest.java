/*
 * SonarQube Java
 * Copyright (C) 2013-2024 SonarSource SA
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
package org.sonar.java.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.utils.Version;
import org.sonar.java.JavaFrontend;
import org.sonar.java.Measurer;
import org.sonar.java.SonarComponents;
import org.sonar.java.checks.naming.BadMethodNameCheck;
import org.sonar.java.classpath.ClasspathForMain;
import org.sonar.java.classpath.ClasspathForTest;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuickFixesResolutionTest {

  private SensorContextTester sensorContext;
  private final TestIssueFilter mainCodeIssueScannerAndFilter = new TestIssueFilter();
  private final TestIssueFilter testCodeIssueScannerAndFilter = new TestIssueFilter();

  public static final Version LATEST_SONARLINT_API_VERSION = Version.create(8, 18);
  public static final SonarRuntime SONARLINT_RUNTIME = SonarRuntimeImpl.forSonarLint(LATEST_SONARLINT_API_VERSION);

  private static final Logger LOG = LoggerFactory.getLogger(QuickFixesResolutionTest.class);

  private static final Path PROJECT_LOCATION = Paths.get("../../java-checks-test-sources/");

  private static final Path TEST_PATH = Paths.get("../../java-checks-test-sources/default/src/main/java/checks/naming/");

  @ClassRule
  public static TemporaryFolder tmpProjectClone = new TemporaryFolder();

  private static final Set<String> PATHS_TO_INSPECT = Set.of(
    "default/src/main/java",
    "aws/src/main/java",
    "java-17/src/main/java"
  );

  private static final String MVN = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "mvn.cmd" : "mvn";

  @Test
  public void testCompilationAfterQuickfixes() throws Exception {

    cloneJavaCheckTestSources();

    scan(new MapSettings(), SONARLINT_RUNTIME, collectJavaFiles(TEST_PATH.toAbsolutePath().toString()));

    Process process = new ProcessBuilder(MVN, "compile")
      .directory(tmpProjectClone.getRoot().toPath().toFile())
      .inheritIO()
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .start();
    int exitCode = process.waitFor();

    // Compilation should be successful
    assertThat(exitCode).isEqualTo(0);
  }

  private static void cloneJavaCheckTestSources() throws Exception {
    try (Stream<Path> paths = Files.walk(PROJECT_LOCATION)) {
      paths.forEach(source -> {
        try {
          Path target = tmpProjectClone.getRoot().toPath().resolve(PROJECT_LOCATION.relativize(source));
          Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      });
    }
  }

  private static List<InputFile> collectJavaFiles(String directory) {
    Path start = Paths.get(directory);
    int maxDepth = Integer.MAX_VALUE; // this is to say that it should search as deep as possible
    try (Stream<Path> stream = Files.walk(start, maxDepth)) {
      return stream
        .filter(path -> path.toString().endsWith(".java"))
        .map(path -> getInputFile(directory, path.toFile()))
        .collect(Collectors.toList());
    } catch (IOException e) {
      LOG.error("Unable to read " + directory);
    }
    return Collections.emptyList();
  }

  private static InputFile getInputFile(String moduleKey, File file) {
    try {
      return new TestInputFileBuilder(moduleKey, file.getPath())
        .setContents(new String(Files.readAllBytes(file.toPath()), UTF_8))
        .setCharset(UTF_8)
        .setLanguage("java")
        .setType(InputFile.Type.MAIN)
        .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<InputFile> scan(MapSettings settings, SonarRuntime sonarRuntime, List<InputFile> inputFiles) throws IOException {
    if (sensorContext == null) {
      File baseDir = tmpProjectClone.getRoot().getAbsoluteFile();
      sensorContext = SensorContextTester.create(baseDir);
      sensorContext.setSettings(settings);
    }
    sensorContext.setRuntime(sonarRuntime);

    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    ClasspathForMain javaClasspath = mock(ClasspathForMain.class);
    ClasspathForTest javaTestClasspath = mock(ClasspathForTest.class);

    CheckFactory checkFactory = mock(CheckFactory.class);
    Checks<Object> checks = mock(Checks.class);
    when(checks.ruleKey(any(JavaCheck.class))).thenReturn(RuleKey.of("java", RuleAnnotationUtils.getRuleKey(BadMethodNameCheck.class)));
    when(checkFactory.create(anyString())).thenReturn(checks);

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, sensorContext.fileSystem(), javaClasspath, javaTestClasspath,
      checkFactory, ActiveRules.);
    sonarComponents.setSensorContext(sensorContext);
    sonarComponents.mainChecks().add(mainCodeIssueScannerAndFilter);
    //sonarComponents.registerMainChecks(CheckList.REPOSITORY_KEY, CheckList.getJavaChecks());
    sonarComponents.testChecks().add(testCodeIssueScannerAndFilter);
    JavaVersion javaVersion = new JavaVersionImpl(21);
//      settings.asConfig().get(JavaVersion.SOURCE_VERSION)
//      .map(JavaVersionImpl::fromString)
//      .orElse(new JavaVersionImpl(21));
    JavaFrontend frontend = new JavaFrontend(javaVersion, sonarComponents, new Measurer(sensorContext, mock(NoSonarFilter.class)), mock(JavaResourceLocator.class),
      null, mainCodeIssueScannerAndFilter);
    frontend.scan(inputFiles, Collections.emptyList(), Collections.emptyList());

    return inputFiles;
  }

  private class TestIssueFilter implements JavaFileScanner, SonarJavaIssueFilter, EndOfAnalysis {
    CompilationUnitTree lastScannedTree = null;
    int scanFileInvocationCount = 0;
    int endOfAnalysisInvocationCount = 0;
    JavaFileScannerContext scannerContext;
    boolean isCancelled = false;
    RuntimeException exceptionDuringScan = null;

    @Override
    public void scanFile(JavaFileScannerContext scannerContext) {
      this.scannerContext = scannerContext;
      scanFileInvocationCount++;
      lastScannedTree = scannerContext.getTree();
      if (isCancelled) {
        sensorContext.setCancelled(true);
      }
      if (exceptionDuringScan != null) {
        RuntimeException ex = exceptionDuringScan;
        exceptionDuringScan = null;
        throw ex;
      }
    }

    @Override
    public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
      return true;
    }

    @Override
    public void endOfAnalysis(ModuleScannerContext context) {
      endOfAnalysisInvocationCount++;
    }
  }

}
