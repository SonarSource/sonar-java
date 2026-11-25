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
package org.sonar.java.semsitter;

import java.util.Objects;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.diagnostics.Warning;
import org.sonar.plugins.java.api.tree.Tree;

public final class JWarning extends JProblem implements Warning {

  private final Tree syntaxTree;

  JWarning(String message, ProblemKind type, Tree syntaxTree) {
    super(message, type);
    this.syntaxTree = syntaxTree;
  }

  @Override
  public Tree syntaxTree() {
    return syntaxTree;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && syntaxTree == ((JWarning) o).syntaxTree;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), System.identityHashCode(syntaxTree));
  }

}
