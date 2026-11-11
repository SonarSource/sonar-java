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

import javax.annotation.Nullable;

public final class JParseResult {

  private final Exception exception;
  private final JavaTree.CompilationUnitTreeImpl tree;

  private JParseResult(@Nullable Exception exception, @Nullable JavaTree.CompilationUnitTreeImpl tree) {
    this.exception = exception;
    this.tree = tree;
  }

  public static JParseResult success(JavaTree.CompilationUnitTreeImpl tree) {
    return new JParseResult(null, tree);
  }

  public static JParseResult failure(Exception exception) {
    return new JParseResult(exception, null);
  }

  public JavaTree.CompilationUnitTreeImpl get() throws Exception {
    if (exception != null) {
      throw exception;
    }
    return tree;
  }

}
