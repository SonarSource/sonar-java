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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.jdt.JDTParser;
import org.sonar.plugins.java.api.JavaVersion;

public interface JParser {

  JavaVersion MAXIMUM_SUPPORTED_JAVA_VERSION = new JavaVersionImpl(JavaVersionImpl.MAX_SUPPORTED, true);

  enum Mode {
    BATCH,
    FILE_BY_FILE;

    public JParser create(JavaVersion javaVersion, List<File> classpath) {
      return create(javaVersion, classpath, false);
    }

    public JParser create(JavaVersion javaVersion, List<File> classpath, boolean shouldIgnoreUnnamedModuleForSplitPackage) {
      return JDTParser.create(this, javaVersion, classpath, shouldIgnoreUnnamedModuleForSplitPackage);
    }
  }

  JavaTree.CompilationUnitTreeImpl parse(String version, String unitName, String source);

  void parse(Iterable<InputFile> inputFiles, BooleanSupplier isCanceled, AnalysisProgress analysisProgress, BiConsumer<InputFile, JParseResult> action);

}
