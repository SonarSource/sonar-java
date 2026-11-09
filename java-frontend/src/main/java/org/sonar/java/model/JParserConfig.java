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
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.psi.PsiParserConfig;
import org.sonar.plugins.java.api.JavaVersion;

public interface JParserConfig {

  JavaVersion MAXIMUM_SUPPORTED_JAVA_VERSION = new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED, true);

  JParser astParser();

  JavaTree.CompilationUnitTreeImpl parse(String unitName, String source);

  void parse(Iterable<? extends InputFile> inputFiles, BooleanSupplier isCanceled,
    AnalysisProgress analysisProgress, BiConsumer<InputFile, Result> action);

  enum Mode {
    BATCH,
    FILE_BY_FILE;

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath) {
      return create(javaVersion, classpath, false);
    }

    public JParserConfig create(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      return PsiParserConfig.create(this, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
    }
  }

  final class Result {
    private final Exception exception;
    private final JavaTree.CompilationUnitTreeImpl tree;

    private Result(@Nullable Exception exception, @Nullable JavaTree.CompilationUnitTreeImpl tree) {
      this.exception = exception;
      this.tree = tree;
    }

    public static Result success(JavaTree.CompilationUnitTreeImpl tree) {
      return new Result(null, tree);
    }

    public static Result failure(Exception exception) {
      return new Result(exception, null);
    }

    public JavaTree.CompilationUnitTreeImpl get() throws Exception {
      if (exception != null) {
        throw exception;
      }
      return tree;
    }
  }

  @FunctionalInterface
  interface ParserConfigConstructor {
    JParserConfig apply(JavaVersion version, List<File> files, Boolean shouldIgnoreUnnamedModuleForSplitPackage);
  }

}
