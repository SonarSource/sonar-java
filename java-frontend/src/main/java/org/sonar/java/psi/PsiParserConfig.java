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
package org.sonar.java.psi;

import com.intellij.openapi.progress.ProcessCanceledException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.concurrent.CancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonarsource.analyzer.commons.ProgressReport;
import org.sonarsource.performance.measure.PerformanceMeasure;

/** Creates Sonar ASTs whose explicit cleaners retain PSI for subsequent lazy semantic queries. */
public final class PsiParserConfig implements JParserConfig {
  private static final Logger LOG = LoggerFactory.getLogger(PsiParserConfig.class);
  private final Mode mode;
  private final JavaPsiConfiguration configuration;

  private PsiParserConfig(Mode mode, JavaVersion version, List<File> classpath, boolean ignoreUnnamedModule) {
    this.mode = mode;
    configuration = new JavaPsiConfiguration(null, classpath.stream().map(File::toPath).toList(),
      Integer.parseInt(version.effectiveJavaVersionAsString()), version.arePreviewFeaturesEnabled(), ignoreUnnamedModule);
    if (ignoreUnnamedModule) {
      LOG.info("The Java analyzer will ignore the unnamed module for split packages.");
    }
  }

  public static JParserConfig create(Mode mode, JavaVersion version, List<File> classpath, boolean ignoreUnnamedModule) {
    return new PsiParserConfig(mode, version, classpath, ignoreUnnamedModule);
  }

  @Override
  public JParser astParser() {
    return (version, name, source) -> parse(new JavaPsiConfiguration(configuration.runtime(), configuration.classpath(),
      Integer.parseInt(version), configuration.previewEnabled(), configuration.ignoreUnnamedModuleForSplitPackage()), name, source);
  }

  @Override
  public JavaTree.CompilationUnitTreeImpl parse(String unitName, String source) {
    return parse(configuration, unitName, source);
  }

  private static JavaTree.CompilationUnitTreeImpl parse(JavaPsiConfiguration configuration, String unitName, String source) {
    return cancellable(() -> {
      var snapshot = new JavaPsiEnvironment.SourceSnapshot(Path.of(unitName), PsiTokenIndex.unicodeInput(source).text(), StandardCharsets.UTF_8);
      var environment = JavaPsiEnvironment.open(configuration, List.of(snapshot), () -> false);
      try (var session = new PsiAnalysisSession(environment)) {
        var tree = environment.read((project, files) -> PsiParser.convert(environment, project, files.getFirst(), configuration, source, () -> {}));
        session.attach(tree);
        return tree;
      }
    });
  }

  @Override
  public void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
    AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action) {
    List<InputFile> inputs = new ArrayList<>();
    inputFiles.forEach(inputs::add);
    if (mode == Mode.BATCH) {
      batch(inputs, isCanceled, analysisProgress, action);
    } else {
      fileByFile(inputs, isCanceled, action);
    }
  }

  private void batch(List<InputFile> inputs, BooleanSupplier isCanceled, AnalysisProgress progress, BiConsumer<InputFile, Result> action) {
    var duration = PerformanceMeasure.start("ParseAsBatch");
    var execution = new ExecutionTimeReport();
    PsiAnalysisSession ownership = null;
    boolean completed = false;
    int processed = 0;
    try (var reporting = new PsiBatchProgress(progress, inputs.size())) {
      checkCanceled(isCanceled);
      var sources = new ArrayList<String>();
      var snapshots = new ArrayList<JavaPsiEnvironment.SourceSnapshot>();
      for (var input : inputs) {
        checkCanceled(isCanceled);
        String source;
        try {
          source = input.contents();
        } catch (IOException failure) {
          throw new UncheckedIOException(failure);
        }
        sources.add(source);
        snapshots.add(new JavaPsiEnvironment.SourceSnapshot(Path.of(input.absolutePath()), PsiTokenIndex.unicodeInput(source).text(), input.charset()));
      }
      if (inputs.isEmpty()) {
        completed = true;
        return;
      }
      var session = cancellable(() -> JavaPsiEnvironment.open(configuration, snapshots, isCanceled));
      ownership = new PsiAnalysisSession(session);
      Runnable cleaner = () -> {};
      for (int i = 0; i < inputs.size(); i++) {
        checkCanceled(isCanceled);
        var input = inputs.get(i);
        var source = sources.get(i);
        int index = i;
        execution.start(input);
        Result result;
        try {
          var tree = cancellable(() -> session.read((project, files) ->
            PsiParser.convert(session, project, files.get(index), configuration, source, cleaner)));
          ownership.attach(tree);
          result = Result.success(tree);
        } catch (CancellationException failure) {
          throw failure;
        } catch (Exception failure) {
          result = Result.failure(failure);
        }
        action.accept(input, result);
        processed++;
        reporting.nextFile();
        execution.end();
      }
      completed = true;
    } catch (CancellationException failure) {
      LOG.info("Batch processing: Cancelled!");
      throw failure;
    } catch (RuntimeException failure) {
      if (ownership != null) {
        ownership.abort();
        ownership.close();
        ownership = null;
      }
      LOG.warn("Unexpected {}: {}", failure.getClass().getSimpleName(), failure.getMessage());
      var pending = inputs.subList(processed, inputs.size());
      if (!pending.isEmpty()) {
        LOG.warn("Fallback to file by file analysis for {} files", pending.size());
        fileByFile(pending, isCanceled, action);
      }
      completed = true;
    } finally {
      if (ownership != null) {
        if (!completed) {
          ownership.abort();
        }
        ownership.close();
      }
      duration.stop();
      execution.reportAsBatch();
    }
  }

  private void fileByFile(List<InputFile> inputs, BooleanSupplier isCanceled, BiConsumer<InputFile, Result> action) {
    var report = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    var execution = new ExecutionTimeReport();
    report.start(inputs.stream().map(InputFile::toString).toList());
    boolean completed = false;
    try {
      for (var input : inputs) {
        if (isCanceled.getAsBoolean()) {
          return;
        }
        checkCanceled(() -> false);
        execution.start(input);
        JavaTree.CompilationUnitTreeImpl tree = null;
        var duration = PerformanceMeasure.start("JParser");
        Result result;
        try {
          tree = parse(input.filename(), input.contents());
          result = Result.success(tree);
        } catch (CancellationException failure) {
          throw failure;
        } catch (Exception failure) {
          result = Result.failure(failure);
        } finally {
          duration.stop();
        }
        try {
          action.accept(input, result);
        } catch (RuntimeException | Error failure) {
          if (tree != null) {
            tree.getEnvironmentCleaner().run();
          }
          throw failure;
        }
        execution.end();
        report.nextFile();
      }
      completed = true;
    } finally {
      if (completed) {
        report.stop();
      } else {
        report.cancel();
      }
      execution.report();
    }
  }

  /** Translates PSI cancellation to the frontend cancellation contract. */
  static <T> T cancellable(Supplier<T> action) {
    try {
      checkCanceled(() -> false);
      return action.get();
    } catch (ProcessCanceledException failure) {
      var canceled = new CancellationException(failure.getMessage());
      canceled.initCause(failure);
      throw canceled;
    }
  }

  private static void checkCanceled(BooleanSupplier canceled) {
    if (Thread.currentThread().isInterrupted() || canceled.getAsBoolean()) {
      throw new CancellationException("Java analysis cancelled");
    }
  }
}
