/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.ProgressReport;
import org.sonarsource.performance.measure.PerformanceMeasure;

public abstract class JParserConfig {

  public static final JavaVersion MAXIMUM_SUPPORTED_JAVA_VERSION = new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED, true);

  private static final Logger LOG = LoggerFactory.getLogger(JParserConfig.class);

  final JavaVersion javaVersion;
  final List<File> classpath;
  final boolean shouldIgnoreUnnamedModuleForSplitPackage;

  private JParserConfig(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
    this.javaVersion = javaVersion;
    this.classpath = classpath;
    this.shouldIgnoreUnnamedModuleForSplitPackage = shouldIgnoreUnnamedModuleForSplitPackage;
  }

  public abstract void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
    AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action);

  public enum Mode {
    BATCH(Batch::new),
    FILE_BY_FILE(FileByFile::new);

    private final ParserConfigConstructor supplier;

    Mode(ParserConfigConstructor supplier) {
      this.supplier = supplier;
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath) {
      return create(javaVersion, classpath, false);
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      if (shouldIgnoreUnnamedModuleForSplitPackage) {
        LOG.info("The Java analyzer will ignore the unnamed module for split packages.");
      }
      return supplier.apply(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
    }
  }

  public static class Result {
    private final Exception e;
    private final JavaTree.CompilationUnitTreeImpl t;

    private Result(Exception e) {
      this.e = e;
      this.t = null;
    }

    private Result(JavaTree.CompilationUnitTreeImpl t) {
      this.e = null;
      this.t = t;
    }

    public JavaTree.CompilationUnitTreeImpl get() throws Exception {
      if (e != null) {
        throw e;
      }
      return t;
    }
  }

  @VisibleForTesting
  static class Batch extends JParserConfig {

    Batch(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
      AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action) {
      ExecutionTimeReport executionTimeReport = new ExecutionTimeReport();
      PerformanceMeasure.Duration batchPerformance = PerformanceMeasure.start("ParseAsBatch");
      try {
        for (InputFile inputFile : inputFiles) {
          if (isCanceled.getAsBoolean()) {
            break;
          }
          executionTimeReport.start(inputFile);
          action.accept(inputFile, parseFile(inputFile));
          executionTimeReport.end();
        }
      } finally {
        batchPerformance.stop();
        executionTimeReport.reportAsBatch();
      }
    }

  }

  private static class FileByFile extends JParserConfig {

    private FileByFile(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      super(javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
    }

    @Override
    public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
      AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action) {
      boolean successfullyCompleted = false;
      boolean cancelled = false;

      ExecutionTimeReport executionTimeReport = new ExecutionTimeReport();
      ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
      List<String> filesNames = StreamSupport.stream(inputFiles.spliterator(), false)
        .map(InputFile::toString)
        .toList();
      progressReport.start(filesNames);
      try {
        for (InputFile inputFile : inputFiles) {
          if (isCanceled.getAsBoolean()) {
            cancelled = true;
            break;
          }
          executionTimeReport.start(inputFile);
          action.accept(inputFile, parseFile(inputFile));
          executionTimeReport.end();
          progressReport.nextFile();
        }
        successfullyCompleted = !cancelled;
      } finally {
        if (successfullyCompleted) {
          progressReport.stop();
        } else {
          progressReport.cancel();
        }
        executionTimeReport.report();
      }
    }
  }

  final Result parseFile(InputFile inputFile) {
    PerformanceMeasure.Duration parseDuration = PerformanceMeasure.start("JParser");
    try {
      return new Result(JParser.parse(javaVersion.effectiveJavaVersionAsString(), inputFile.filename(), inputFile.contents()));
    } catch (Exception e) {
      return new Result(e);
    } finally {
      parseDuration.stop();
    }
  }

  @VisibleForTesting
  static boolean shouldEnablePreviewFlag(JavaVersion currentVersion) {
    return currentVersion.arePreviewFeaturesEnabled();
  }

  @FunctionalInterface
  public interface ParserConfigConstructor {
    JParserConfig apply(JavaVersion version, List<File> files, Boolean shouldIgnoreUnnamedModuleForSplitPackage);
  }

}
