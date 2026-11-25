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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.plugins.java.api.JavaVersion;

public interface JParser {

  JavaVersion MAXIMUM_SUPPORTED_JAVA_VERSION = new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED, true);

  enum Mode {
    BATCH,
    FILE_BY_FILE;

    public JParser create(JavaVersion javaVersion, List<File> classpath) {
      return create(javaVersion, classpath, false, true);
    }

    public JParser create(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage, boolean shouldUseJdkReleaseOption) {
      return JParserProviders.create(this, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage, shouldUseJdkReleaseOption);
    }
  }

  static JavaTree.CompilationUnitTreeImpl parse(InputFile inputFile, List<File> classpath, JavaVersion javaVersion) {
    List<JParseResult> results = new ArrayList<>(1);
    Mode.FILE_BY_FILE.create(javaVersion, classpath).parse(List.of(inputFile), () -> false, new AnalysisProgress(1), (input, result) -> results.add(result));
    try {
      return results.getFirst().get();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Unable to parse file '" + inputFile.absolutePath() + "'", e);
    }
  }

  void parse(Iterable<InputFile> inputFiles, BooleanSupplier isCanceled, AnalysisProgress analysisProgress, BiConsumer<InputFile, JParseResult> action);

}
