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

import com.intellij.openapi.progress.ProcessCanceledException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.eclipse.core.runtime.OperationCanceledException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.ProgressMonitor;
import org.sonar.java.psi.JavaPsiConfiguration;
import org.sonar.java.psi.JavaPsiEnvironment;

final class PsiFrontendBackend implements FrontendBackend {
  @Override
  public FrontendSession open(FrontendConfiguration configuration) {
    return new Session(configuration);
  }

  static <T> T cancellable(Supplier<T> action) {
    try {
      if (Thread.currentThread().isInterrupted()) {
        throw new OperationCanceledException("Java analysis interrupted");
      }
      return action.get();
    } catch (ProcessCanceledException failure) {
      var canceled = new OperationCanceledException(failure.getMessage());
      canceled.initCause(failure);
      throw canceled;
    }
  }

  private static final class Session extends FrontendSession {
    private final JavaPsiConfiguration configuration;

    private Session(FrontendConfiguration configuration) {
      this.configuration = new JavaPsiConfiguration(null, configuration.classpath().stream().map(File::toPath).toList(),
        Integer.parseInt(configuration.sourceVersion()), configuration.previewEnabled(), configuration.ignoreUnnamedModuleForSplitPackage());
    }

    @Override
    JavaTree.CompilationUnitTreeImpl parse(String unitName, String source) {
      return cancellable(() -> {
        var snapshot = new JavaPsiEnvironment.SourceSnapshot(Path.of(unitName), PsiTokenIndex.unicodeInput(source).text(), StandardCharsets.UTF_8);
        var environment = JavaPsiEnvironment.open(configuration, List.of(snapshot), () -> false);
        var cleaner = own(environment::close);
        return environment.read((project, files) -> JParser.convert(environment, project, files.getFirst(), configuration, source, cleaner));
      });
    }

    /** Keeps all decoded inputs visible in one owned project while delivering callbacks in input order. */
    @Override
    void parseBatch(FrontendBatch inputs, ProgressMonitor monitor, BiConsumer<InputFile, ParsedUnit> action) {
      monitor.beginTask("Parse Java sources", inputs.inputs().size());
      cancellable(() -> {
        BooleanSupplier canceled = () -> monitor.isCanceled() || Thread.currentThread().isInterrupted();
        checkCanceled(canceled);
        var snapshots = new ArrayList<JavaPsiEnvironment.SourceSnapshot>();
        var sources = new ArrayList<String>();
        for (var input : inputs.inputs()) {
          checkCanceled(canceled);
          try {
            var source = input.contents();
            sources.add(source);
            snapshots.add(new JavaPsiEnvironment.SourceSnapshot(Path.of(input.absolutePath()), PsiTokenIndex.unicodeInput(source).text(), input.charset()));
          } catch (IOException failure) {
            throw new UncheckedIOException(failure);
          }
        }
        if (snapshots.isEmpty()) {
          return null;
        }
        var environment = JavaPsiEnvironment.open(configuration, snapshots, canceled);
        var cleaner = own(environment::close);
        environment.read((project, files) -> {
          for (int i = 0; i < inputs.inputs().size(); i++) {
            checkCanceled(canceled);
            var input = inputs.inputs().get(i);
            var file = files.get(i);
            var source = sources.get(i);
            action.accept(input, () -> cancellable(() -> JParser.convert(environment, project, file, configuration, source, cleaner)));
            monitor.worked(1);
          }
          checkCanceled(canceled);
          return null;
        });
        return null;
      });
    }

    private static void checkCanceled(BooleanSupplier canceled) {
      if (canceled.getAsBoolean()) {
        throw new OperationCanceledException("Java analysis cancelled");
      }
    }
  }
}
