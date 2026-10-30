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

import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.sonar.java.semsitter.SemsitterBridge;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
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
  private SemsitterBridge bridge;

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
      List<InputFile> files = new ArrayList<>();
      inputFiles.forEach(files::add);
      try {
        openBridge(files);
        for (InputFile inputFile : files) {
          if (isCanceled.getAsBoolean()) {
            break;
          }
          executionTimeReport.start(inputFile);
          action.accept(inputFile, parseFile(inputFile));
          executionTimeReport.end();
        }
      } finally {
        closeBridge();
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
      List<InputFile> files = new ArrayList<>();
      inputFiles.forEach(files::add);
      List<String> filesNames = files.stream().map(InputFile::toString).toList();
      progressReport.start(filesNames);
      try {
        openBridge(files);
        for (InputFile inputFile : files) {
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
        closeBridge();
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
      if (bridge == null) {
        return new Result(JParser.parse(javaVersion.effectiveJavaVersionAsString(), inputFile.filename(), inputFile.contents()));
      }
      JsonObject document = bridge.cst(inputFile.path().toAbsolutePath().toString());
      JSema sema = new JSema();
      sema.ingestDocument(document);
      sema.loadTypes(names -> {
        try {
          return bridge.types(names);
        } catch (IOException failure) {
          throw new UncheckedIOException(failure);
        }
      });
      return new Result(SemsitterTranslator.translate(sema, document));
    } catch (Exception e) {
      return new Result(e);
    } finally {
      parseDuration.stop();
    }
  }

  void openBridge(List<? extends InputFile> files) {
    try {
      bridge = SemsitterBridge.start(JParser.bridgePath());
      if (!files.isEmpty()) {
        Path root = files.get(0).path().toAbsolutePath().getParent();
        bridge.analyze(
          root == null ? Path.of(".") : root,
          files.stream().map(file -> file.path().toAbsolutePath()).toList(),
          classpath.stream().map(File::getAbsolutePath).toList());
      }
    } catch (Exception failure) {
      closeBridge();
      LOG.warn("SemSitter bridge did not start ({})", failure.toString());
    }
  }

  void closeBridge() {
    if (bridge != null) {
      bridge.close();
      bridge = null;
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
