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
package org.sonar.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.collections.CollectionUtils;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.performance.measure.PerformanceMeasure;
import org.sonarsource.performance.measure.PerformanceMeasure.Duration;

public class JavaFrontend {

  private static final Logger LOG = Loggers.get(JavaFrontend.class);

  private final JavaVersion javaVersion;
  private final SonarComponents sonarComponents;
  private final List<File> globalClasspath;
  private final JavaAstScanner astScanner;
  private final JavaAstScanner astScannerForTests;
  private final JavaAstScanner astScannerForGeneratedFiles;

  public JavaFrontend(JavaVersion javaVersion, @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                     JavaResourceLocator javaResourceLocator, @Nullable SonarJavaIssueFilter postAnalysisIssueFilter, JavaCheck... visitors) {
    this.javaVersion = javaVersion;
    this.sonarComponents =sonarComponents;
    List<JavaCheck> commonVisitors = new ArrayList<>();
    commonVisitors.add(javaResourceLocator);
    if (postAnalysisIssueFilter != null) {
      commonVisitors.add(postAnalysisIssueFilter);
    }

    Iterable<JavaCheck> codeVisitors = ListUtils.concat(commonVisitors, Arrays.asList(visitors));
    Collection<JavaCheck> testCodeVisitors = new ArrayList<>(commonVisitors);
    if (measurer != null) {
      Iterable<JavaCheck> measurers = Collections.singletonList(measurer);
      codeVisitors = ListUtils.concat(measurers, codeVisitors);
      testCodeVisitors.add(measurer.new TestFileMeasurer());
    }
    List<File> classpath = new ArrayList<>();
    List<File> testClasspath = new ArrayList<>();
    List<JavaCheck> jspCodeVisitors = new ArrayList<>();
    List<File> jspClasspath = new ArrayList<>();
    boolean inAndroidContext = false;
    if (sonarComponents != null) {
      if(!sonarComponents.isSonarLintContext()) {
        codeVisitors = ListUtils.concat(codeVisitors, Arrays.asList(new FileLinesVisitor(sonarComponents), new SyntaxHighlighterVisitor(sonarComponents)));
        testCodeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents));
      }
      classpath = sonarComponents.getJavaClasspath();
      testClasspath = sonarComponents.getJavaTestClasspath();
      jspClasspath = sonarComponents.getJspClasspath();
      testCodeVisitors.addAll(sonarComponents.testChecks());
      jspCodeVisitors = sonarComponents.jspChecks();
      inAndroidContext = sonarComponents.inAndroidContext();
    }
    globalClasspath = Stream.of(classpath, testClasspath, jspClasspath)
      .flatMap(Collection::stream).distinct().collect(Collectors.toList());

    //AstScanner for main files
    astScanner = new JavaAstScanner(sonarComponents);
    astScanner.setVisitorBridge(createVisitorBridge(codeVisitors, classpath, javaVersion, sonarComponents, inAndroidContext));

    //AstScanner for test files
    astScannerForTests = new JavaAstScanner(sonarComponents);
    astScannerForTests.setVisitorBridge(createVisitorBridge(testCodeVisitors, testClasspath, javaVersion, sonarComponents, inAndroidContext));

    //AstScanner for generated files
    astScannerForGeneratedFiles = new JavaAstScanner(sonarComponents);
    astScannerForGeneratedFiles.setVisitorBridge(createVisitorBridge(jspCodeVisitors, jspClasspath, javaVersion, sonarComponents, inAndroidContext));
  }

  private static VisitorsBridge createVisitorBridge(
    Iterable<JavaCheck> codeVisitors, List<File> classpath, JavaVersion javaVersion, @Nullable SonarComponents sonarComponents, boolean inAndroidContext) {
    VisitorsBridge visitorsBridge = new VisitorsBridge(codeVisitors, classpath, sonarComponents);
    visitorsBridge.setJavaVersion(javaVersion);
    visitorsBridge.setInAndroidContext(inAndroidContext);
    return visitorsBridge;
  }

  @VisibleForTesting
  boolean analysisCancelled() {
    return sonarComponents != null && sonarComponents.analysisCancelled();
  }

  public void scan(Iterable<InputFile> sourceFiles, Iterable<InputFile> testFiles, Iterable<? extends InputFile> generatedFiles) {
    if (isBatchModeEnabled()) {
      // generated files are intentionally ignored in batch mode
      scanAsBatch(sourceFiles, testFiles);
    } else {
      scanAndMeasureTask(sourceFiles, astScanner::scan, "Main");
      scanAndMeasureTask(testFiles, astScannerForTests::scan, "Test");
      scanAndMeasureTask(generatedFiles, astScannerForGeneratedFiles::scan, "Generated");
    }
  }

  private void scanAsBatch(Iterable<InputFile>... sourceFiles) {
    try {
      List<InputFile> allFiles = new ArrayList<>();
      Arrays.stream(sourceFiles).forEach(files -> files.forEach(allFiles::add));
      try {
        JParserConfig.Mode.BATCH
          .create(JParserConfig.effectiveJavaVersion(javaVersion), globalClasspath)
          .parse(allFiles, this::analysisCancelled, this::scanAsBatchCallback);
      } finally {
        astScanner.endOfAnalysis();
        astScannerForTests.endOfAnalysis();
        astScannerForGeneratedFiles.endOfAnalysis();
      }
    } catch (AnalysisException e) {
      throw e;
    } catch (Exception e) {
      astScanner.checkInterrupted(e);
      LOG.error("Batch Mode failed, analysis of Java Files stopped.", e);
      if (astScanner.shouldFailAnalysis()) {
        throw new AnalysisException("Batch Mode failed, analysis of Java Files stopped.", e);
      }
    }
  }

  private void scanAsBatchCallback(InputFile inputFile, JParserConfig.Result result) {
    JavaAstScanner scanner = inputFile.type() == InputFile.Type.TEST ? astScannerForTests : astScanner;
    Duration duration = PerformanceMeasure.start(inputFile.type() == InputFile.Type.TEST ? "Test" : "Main");
    scanner.simpleScan(inputFile, result, ast -> {
      // Do nothing. In batch mode, can not clean the ast as it will be used in later processing.
    });
    duration.stop();
  }

  private boolean isBatchModeEnabled() {
    return sonarComponents != null && sonarComponents.isBatchModeEnabled();
  }

  private static <T> void scanAndMeasureTask(Iterable<T> files, Consumer<Iterable<T>> action, String descriptor) {
    if (CollectionUtils.size(files) > 0) {
      Duration mainDuration = PerformanceMeasure.start(descriptor);
      Profiler profiler = Profiler.create(LOG).startInfo(String.format("Java \"%s\" source files AST scan", descriptor));

      action.accept(files);

      profiler.stopInfo();
      mainDuration.stop();
    } else {
      LOG.info(String.format("No \"%s\" source files to scan.", descriptor));
    }
  }
}
