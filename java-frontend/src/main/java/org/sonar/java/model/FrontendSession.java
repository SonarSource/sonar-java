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

import java.io.IOException;
import java.util.function.BiConsumer;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.ProgressMonitor;

abstract class FrontendSession implements AutoCloseable {

  private final FrontendResources resources = new FrontendResources();

  final Runnable own(Runnable cleaner) {
    return resources.own(cleaner);
  }

  abstract JavaTree.CompilationUnitTreeImpl parse(String unitName, String source);

  abstract void parseBatch(FrontendBatch inputs, ProgressMonitor monitor, BiConsumer<InputFile, ParsedUnit> action);

  @Override
  public void close() {
    resources.close();
  }

  @FunctionalInterface
  interface ParsedUnit {
    JavaTree.CompilationUnitTreeImpl convert() throws IOException;
  }
}
