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

import java.util.Collections;
import java.util.Objects;

public final class JParser {

  private JParser() {
  }

  public static JavaTree.CompilationUnitTreeImpl parse(String version, String unitName, String source) {
    Objects.requireNonNull(version);
    Objects.requireNonNull(unitName);
    Objects.requireNonNull(source);
    int line = 1;
    int column = 0;
    for (int i = 0; i < source.length(); i++) {
      if (source.charAt(i) == '\n') {
        line++;
        column = 0;
      } else {
        column++;
      }
    }
    InternalSyntaxToken eof = new InternalSyntaxToken(line, column, "", Collections.emptyList(), true);
    JavaTree.CompilationUnitTreeImpl unit = new JavaTree.CompilationUnitTreeImpl(null, Collections.emptyList(), Collections.emptyList(), null, eof);
    unit.sema = new JSema();
    return unit;
  }

}
