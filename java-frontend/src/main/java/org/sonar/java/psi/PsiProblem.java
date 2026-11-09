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

import java.util.Objects;
import org.sonar.plugins.java.api.diagnostics.Problem;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;

class PsiProblem implements Problem {
  private final String message;
  private final ProblemKind type;

  PsiProblem(String message, ProblemKind type) {
    this.message = message;
    this.type = type;
  }

  @Override
  public ProblemKind type() {
    return type;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public String toString() {
    return message;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PsiProblem problem && type == problem.type && message.equals(problem.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message);
  }
}
