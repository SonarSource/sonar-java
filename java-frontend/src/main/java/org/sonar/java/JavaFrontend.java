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
import java.util.Iterator;
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
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonarsource.performance.measure.PerformanceMeasure;
import org.sonarsource.performance.measure.PerformanceMeasure.Duration;

public class JavaFrontend {

  private static final Logger LOG = Loggers.get(JavaFrontend.class);
  private static final String BATCH_ERROR_MESSAGE = "Batch Mode failed, analysis of Java Files stopped.";

  private final JavaVersion javaVersion;
  private final SonarComponents sonarComponents;
  private final List<File> globalClasspath;
  private final JavaAstScanner astScanner;
  private final JavaAstScanner astScannerForTests;
  private final JavaAstScanner astScannerForGeneratedFiles;

  public JavaFrontend(JavaVersion javaVersion, @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                      JavaResourceLocator javaResourceLocator, @Nullable SonarJavaIssueFilter postAnalysisIssueFilter, JavaCheck... visitors) {
    this.javaVersion = javaVersion;
    this.sonarComponents = sonarComponents;
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
      if (!sonarComponents.isSonarLintContext()) {
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
    // SonarLint is not compatible with batch mode, it needs InputFile#contents() and batch mode use InputFile#absolutePath()
    boolean isSonarLint = sonarComponents != null && sonarComponents.isSonarLintContext();
    boolean fileByFileMode = isSonarLint || !isBatchModeEnabled();
    if (fileByFileMode) {
      scanAndMeasureTask(sourceFiles, astScanner::scan, "Main");
      scanAndMeasureTask(testFiles, astScannerForTests::scan, "Test");
      scanAndMeasureTask(generatedFiles, astScannerForGeneratedFiles::scan, "Generated");
    } else if (isAutoScan()) {
      scanAsBatch(new AutoScanBatchContext(), sourceFiles, testFiles);
    } else {
      scanAsBatch(new DefaultBatchModeContext(astScanner, "Main"), sourceFiles);
      scanAsBatch(new DefaultBatchModeContext(astScannerForTests, "Test"), testFiles);
      scanAsBatch(new DefaultBatchModeContext(astScannerForGeneratedFiles, "Generated"), generatedFiles);
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
      files.addAll(astScanner.filterModuleInfo(group).collect(Collectors.toList()));
    }
    try {
      try {
        if (!files.isEmpty()) {
          scanInBatches(context, files);
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
    long batchModeSizeInKB = sonarComponents.getBatchModeSizeInKB();
    if (batchModeSizeInKB < 0L || batchModeSizeInKB >= Long.MAX_VALUE / 1_000L) {
      LOG.debug("Scanning in a single batch");
      scanBatch(context, allInputFiles);
    } else {
      long batchize = batchModeSizeInKB * 1_000L;
      LOG.debug("Scanning with batch size {} B", batchize);
      BatchGenerator generator = new BatchGenerator(allInputFiles.iterator(), batchize);
      while (generator.hasNext()) {
        List<InputFile> batch = generator.next();
        scanBatch(context, batch);
      }
    }
  }

  private <T extends InputFile> void scanBatch(BatchModeContext context, List<T> allFiles) {
    JParserConfig.Mode.BATCH
      .create(JParserConfig.effectiveJavaVersion(javaVersion), context.getClasspath())
      .parse(allFiles, this::analysisCancelled, (input, result) -> scanAsBatchCallback(input, result, context));
  }

  private static void scanAsBatchCallback(InputFile inputFile, JParserConfig.Result result, BatchModeContext context) {
    JavaAstScanner scanner = context.selectScanner(inputFile);
    Duration duration = PerformanceMeasure.start(context.descriptor(inputFile));
    scanner.simpleScan(inputFile, result, ast -> {
      // Do nothing. In batch mode, can not clean the ast as it will be used in later processing.
    });
    duration.stop();
  }

  interface BatchModeContext {
    String descriptor(InputFile input);

    List<File> getClasspath();

    JavaAstScanner selectScanner(InputFile input);

    void endOfAnalysis();
  }

  class AutoScanBatchContext implements BatchModeContext {

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

  static class BatchGenerator {
    public final long batchSizeInBytes;
    private final Iterator<InputFile> source;
    private InputFile buffer = null;


    public BatchGenerator(Iterator<InputFile> source, long batchSizeInBytes) {
      this.source = source;
      this.batchSizeInBytes = batchSizeInBytes;
    }

    public boolean hasNext() {
      return buffer != null || source.hasNext();
    }

    public List<InputFile> next() {
      List<InputFile> batch = new ArrayList<>();
      long batchSize = 0;
      if (buffer != null) {
        batchSize += buffer.file().length();
        batch.add(buffer);
        buffer = null;
        if (batchSize > batchSizeInBytes) {
          return batch;
        }
      }
      if (!source.hasNext()) {
        return batch;
      }
      buffer = source.next();
      batch.add(buffer);
      batchSize += buffer.file().length();
      while (true) {
        if (batchSize > batchSizeInBytes) {
          if (batch.size() == 1) {
            buffer = null;
            return batch;
          }
          return batch.subList(0, batch.size() - 1);
        }
        if (!source.hasNext()) {
          buffer = null;
          return batch;
        }
        buffer = source.next();
        batch.add(buffer);
        batchSize += buffer.file().length();
      }
    }
  }


  @VisibleForTesting
  boolean isBatchModeEnabled() {
    return sonarComponents != null && sonarComponents.isBatchModeEnabled();
  }

  @VisibleForTesting
  boolean isAutoScan() {
    return sonarComponents != null && sonarComponents.isAutoScan();
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
