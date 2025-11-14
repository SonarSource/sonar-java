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
package org.sonar.java;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.caching.CacheContextImpl;
import org.sonar.java.classpath.DependencyVersionInference;
import org.sonar.java.collections.CollectionUtils;
import org.sonar.java.exceptions.ApiMismatchException;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryKey;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.performance.measure.PerformanceMeasure;
import org.sonarsource.performance.measure.PerformanceMeasure.Duration;

import static org.sonar.java.telemetry.TelemetryKey.JAVA_DEPENDENCY_LOMBOK;
import static org.sonar.java.telemetry.TelemetryKey.JAVA_DEPENDENCY_SPRING_BOOT;
import static org.sonar.java.telemetry.TelemetryKey.JAVA_DEPENDENCY_SPRING_WEB;

public class JavaFrontend {

  private static final Logger LOG = LoggerFactory.getLogger(JavaFrontend.class);
  private static final String BATCH_ERROR_MESSAGE = "Batch Mode failed, analysis of Java Files stopped.";

  /** List of libraries, whose presence or absence we want to report. */
  private static final Map<TelemetryKey, String> REPORTED_DEPENDENCIES = Map.of(
    JAVA_DEPENDENCY_LOMBOK, "lombok",
    JAVA_DEPENDENCY_SPRING_BOOT, "spring-boot",
    JAVA_DEPENDENCY_SPRING_WEB, "spring-web"
  );

  private final JavaVersion javaVersion;
  private final SonarComponents sonarComponents;
  private final Telemetry telemetry;
  private final List<File> globalClasspath;
  private final JavaAstScanner astScanner;
  private final JavaAstScanner astScannerForTests;
  private final JavaAstScanner astScannerForGeneratedFiles;

  public JavaFrontend(JavaVersion javaVersion, SonarComponents sonarComponents, Measurer measurer, Telemetry telemetry,
                      JavaResourceLocator javaResourceLocator, @Nullable SonarJavaIssueFilter postAnalysisIssueFilter, JavaCheck... visitors) {
    this.javaVersion = javaVersion;
    this.sonarComponents = sonarComponents;
    this.telemetry = telemetry;
    List<JavaCheck> commonVisitors = new ArrayList<>();
    commonVisitors.add(javaResourceLocator);
    if (postAnalysisIssueFilter != null) {
      commonVisitors.add(postAnalysisIssueFilter);
    }

    List<JavaCheck> codeVisitors = new ArrayList<>();
    codeVisitors.add(measurer);
    codeVisitors.addAll(commonVisitors);
    codeVisitors.addAll(Arrays.asList(visitors));

    List<JavaCheck> testCodeVisitors = new ArrayList<>(commonVisitors);
    testCodeVisitors.add(measurer.new TestFileMeasurer());

    if (!sonarComponents.isSonarLintContext()) {
      codeVisitors.add(new FileLinesVisitor(sonarComponents));
      codeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents));
      testCodeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents));
    }
    List<File> classpath = sonarComponents.getJavaClasspath();
    List<File> testClasspath = sonarComponents.getJavaTestClasspath();
    List<File> jspClasspath = sonarComponents.getJspClasspath();
    testCodeVisitors.addAll(sonarComponents.testChecks());
    List<JavaCheck> jspCodeVisitors = sonarComponents.jspChecks();
    boolean inAndroidContext = sonarComponents.inAndroidContext();

    globalClasspath = Stream.of(classpath, testClasspath, jspClasspath)
      .flatMap(Collection::stream).distinct().toList();

    //AstScanner for main files
    astScanner = new JavaAstScanner(sonarComponents, telemetry, TelemetryKey.JAVA_ANALYSIS_MAIN);
    astScanner.setVisitorBridge(new VisitorsBridge(codeVisitors, classpath, sonarComponents, javaVersion, inAndroidContext));

    //AstScanner for test files
    astScannerForTests = new JavaAstScanner(sonarComponents, telemetry, TelemetryKey.JAVA_ANALYSIS_TEST);
    astScannerForTests.setVisitorBridge(new VisitorsBridge(testCodeVisitors, testClasspath, sonarComponents, javaVersion, inAndroidContext));

    //AstScanner for generated files
    astScannerForGeneratedFiles = new JavaAstScanner(sonarComponents, telemetry, TelemetryKey.JAVA_ANALYSIS_GENERATED);
    astScannerForGeneratedFiles.setVisitorBridge(new VisitorsBridge(jspCodeVisitors, jspClasspath, sonarComponents, javaVersion, inAndroidContext));
  }

  public void scan(Iterable<InputFile> sourceFiles, Iterable<InputFile> testFiles, Iterable<? extends InputFile> generatedFiles) {
    if (canOptimizeScanning()) {
      long successfullyScanned = 0L;
      long total = 0L;

      Map<Boolean, List<InputFile>> mainFilesScannedWithoutParsing = astScanner.scanWithoutParsing(sourceFiles);
      sourceFiles = mainFilesScannedWithoutParsing.get(false);
      successfullyScanned += mainFilesScannedWithoutParsing.get(true).size();
      total += mainFilesScannedWithoutParsing.get(true).size() + mainFilesScannedWithoutParsing.get(false).size();

      Map<Boolean, List<InputFile>> testFilesScannedWithoutParsing = astScannerForTests.scanWithoutParsing(testFiles);
      testFiles = testFilesScannedWithoutParsing.get(false);
      successfullyScanned += testFilesScannedWithoutParsing.get(true).size();
      total += testFilesScannedWithoutParsing.get(true).size() + testFilesScannedWithoutParsing.get(false).size();

      total += StreamSupport.stream(generatedFiles.spliterator(), false).count();

      LOG.info(
        "Server-side caching is enabled. The Java analyzer was able to leverage cached data from previous analyses for {} out of {} files. These files will not be parsed.",
        successfullyScanned,
        total
      );
    } else if (isCacheEnabled()) {
      LOG.info("Server-side caching is enabled. The Java analyzer will not try to leverage data from a previous analysis.");
    } else {
      LOG.info("Server-side caching is not enabled. The Java analyzer will not try to leverage data from a previous analysis.");
    }

    // SonarLint is not compatible with batch mode, it needs InputFile#contents() and batch mode use InputFile#absolutePath()
    boolean fileByFileMode = sonarComponents.isSonarLintContext() || sonarComponents.isFileByFileEnabled();
    if (fileByFileMode) {
      scanAndMeasureTask(sourceFiles, astScanner::scan, "Main");
      scanAndMeasureTask(testFiles, astScannerForTests::scan, "Test");
      scanAndMeasureTask(generatedFiles, astScannerForGeneratedFiles::scan, "Generated");
    } else if (sonarComponents.isAutoScan()) {
      scanAsBatch(new AutoScanBatchContext(), sourceFiles, testFiles);
    } else {
      scanAsBatch(new DefaultBatchModeContext(astScanner, "Main"), sourceFiles);
      scanAsBatch(new DefaultBatchModeContext(astScannerForTests, "Test"), testFiles);
      scanAsBatch(new DefaultBatchModeContext(astScannerForGeneratedFiles, "Generated"), generatedFiles);
    }

    DependencyVersionInference dependencyService = new DependencyVersionInference();
    for (Map.Entry<TelemetryKey, String> dep : REPORTED_DEPENDENCIES.entrySet()) {
      dependencyService
        .infer(dep.getValue(), globalClasspath)
        .ifPresentOrElse(
          version -> telemetry.aggregateAsSortedSet(dep.getKey(), version.toString()),
          () -> telemetry.aggregateAsSortedSet(dep.getKey())
        );
    }
  }

  /**
   * Scans the files given as input in batch mode.
   *
   * The batch size used is determined by configuration.
   * This batch size is then used as a threshold: files are added to a batch until the threshold is passed.
   * Once the threshold is passed, the batch is processed for analysis.
   *
   * If no batch size is configured, the input files are scanned as a single batch.
   *
   * @param inputFiles The collections of files to scan
   */
  private void scanAsBatch(BatchModeContext context, Iterable<? extends InputFile>... inputFiles) {
    List<InputFile> files = new ArrayList<>();
    for (Iterable<? extends InputFile> group : inputFiles) {
      files.addAll(astScanner.filterModuleInfo(group).toList());
    }
    try {
      try {
        if (!files.isEmpty()) {
          scanInBatches(context, files);
        } else if (LOG.isInfoEnabled()) {
          LOG.info("No \"{}\" source files to scan.", context.descriptor());
        }
      } finally {
        context.endOfAnalysis();
      }
    } catch (AnalysisException e) {
      throw e;
    } catch (Exception e) {
      astScanner.checkInterrupted(e);
      astScannerForTests.checkInterrupted(e);
      astScannerForGeneratedFiles.checkInterrupted(e);
      LOG.error(BATCH_ERROR_MESSAGE, e);
      if (astScanner.shouldFailAnalysis()) {
        throw new AnalysisException(BATCH_ERROR_MESSAGE, e);
      }
    }
  }

  private void scanInBatches(BatchModeContext context, List<InputFile> allInputFiles) {
    String logUsingBatch = String.format("Using ECJ batch to parse %d %s java source files", allInputFiles.size(), context.descriptor());
    AnalysisProgress analysisProgress = new AnalysisProgress(allInputFiles.size());
    long batchModeSizeInKB = sonarComponents.getBatchModeSizeInKB();
    if (batchModeSizeInKB < 0L || batchModeSizeInKB >= Long.MAX_VALUE / 1_000L) {
      LOG.info("{} in a single batch.", logUsingBatch);
      scanBatch(context, allInputFiles, analysisProgress);
    } else {
      long batchSize = batchModeSizeInKB * 1_000L;
      LOG.info("{} with batch size {} KB.", logUsingBatch, batchModeSizeInKB);
      BatchGenerator generator = new BatchGenerator(allInputFiles.iterator(), batchSize);
      while (generator.hasNext()) {
        List<InputFile> batch = generator.next();
        scanBatch(context, batch, analysisProgress);
      }
    }
  }

  private <T extends InputFile> void scanBatch(BatchModeContext context, List<T> batchFiles, AnalysisProgress analysisProgress) {
    analysisProgress.startBatch(batchFiles.size());
    Set<Runnable> environmentsCleaners = new HashSet<>();
    JParserConfig.Mode.BATCH
      .create(javaVersion, context.getClasspath(), sonarComponents.shouldIgnoreUnnamedModuleForSplitPackage())
      .parse(batchFiles, sonarComponents::analysisCancelled, analysisProgress, (input, result) -> scanAsBatchCallback(input, result, context, environmentsCleaners));
    // Due to a bug in ECJ, JAR files remain locked after the analysis on Windows, we unlock them manually, at the end of each batches. See SONARJAVA-3609.
    environmentsCleaners.forEach(Runnable::run);
    analysisProgress.endBatch();
  }

  private static void scanAsBatchCallback(InputFile inputFile, JParserConfig.Result result, BatchModeContext context, Set<Runnable> environmentsCleaners) {
    JavaAstScanner scanner = context.selectScanner(inputFile);
    Duration duration = PerformanceMeasure.start(context.descriptor(inputFile));
    scanner.simpleScan(inputFile, result, ast ->
      // In batch mode, we delay the cleaning of the environment as it will be used in later processing.
      environmentsCleaners.add(ast.sema.getEnvironmentCleaner())
    );
    duration.stop();
  }

  interface BatchModeContext {
    String descriptor();

    String descriptor(InputFile input);

    List<File> getClasspath();

    JavaAstScanner selectScanner(InputFile input);

    void endOfAnalysis();
  }

  class AutoScanBatchContext implements BatchModeContext {

    @Override
    public String descriptor() {
      return "Main and Test";
    }

    @Override
    public String descriptor(InputFile input) {
      return input.type() == InputFile.Type.TEST ? "Test" : "Main";
    }

    @Override
    public List<File> getClasspath() {
      return globalClasspath;
    }

    @Override
    public JavaAstScanner selectScanner(InputFile input) {
      return input.type() == InputFile.Type.TEST ? astScannerForTests : astScanner;
    }

    @Override
    public void endOfAnalysis() {
      astScanner.endOfAnalysis();
      astScannerForTests.endOfAnalysis();
      astScannerForGeneratedFiles.endOfAnalysis();
    }

  }

  static class DefaultBatchModeContext implements BatchModeContext {
    private final JavaAstScanner scanner;
    private final String descriptor;

    public DefaultBatchModeContext(JavaAstScanner scanner, String descriptor) {
      this.scanner = scanner;
      this.descriptor = descriptor;
    }

    @Override
    public String descriptor() {
      return descriptor;
    }

    @Override
    public String descriptor(InputFile input) {
      return descriptor;
    }

    @Override
    public List<File> getClasspath() {
      return scanner.getClasspath();
    }

    @Override
    public JavaAstScanner selectScanner(InputFile input) {
      return scanner;
    }

    @Override
    public void endOfAnalysis() {
      scanner.endOfAnalysis();
    }

  }

  private boolean isCacheEnabled() {
    return CacheContextImpl.of(sonarComponents).isCacheEnabled();
  }

  private boolean canOptimizeScanning() {
    try {
      return sonarComponents.canSkipUnchangedFiles() && isCacheEnabled();
    } catch (ApiMismatchException e) {
      return false;
    }
  }

  private static <T> void scanAndMeasureTask(Iterable<T> files, Consumer<Iterable<T>> action, String descriptor) {
    if (CollectionUtils.size(files) > 0) {
      Duration mainDuration = PerformanceMeasure.start(descriptor);

      action.accept(files);

      mainDuration.stop();
    } else {
      LOG.info("No \"{}\" source files to scan.", descriptor);
    }
  }
}
