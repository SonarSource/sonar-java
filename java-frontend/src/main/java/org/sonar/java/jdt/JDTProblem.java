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
package org.sonar.java.jdt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.sonar.plugins.java.api.diagnostics.Problem;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;

class JDTProblem implements Problem {

  private static final String USE_ENABLE_PREVIEW = " Use --enable-preview to enable";

  private static final Map<Integer, ProblemKind> PROBLEM_KINDS = Map.of(
    IProblem.UndefinedType, ProblemKind.UNDEFINED_TYPE,
    IProblem.PreviewFeatureUsed, ProblemKind.PREVIEW_FEATURE_USED,
    IProblem.UnusedImport, ProblemKind.UNUSED_IMPORT,
    IProblem.UnnecessaryCast, ProblemKind.REDUNDANT_CAST,
    IProblem.AssignmentHasNoEffect, ProblemKind.ASSIGNMENT_HAS_NO_EFFECT,
    IProblem.MaskedCatch, ProblemKind.MASKED_CATCH);

  private static final Set<String> COMPILER_OPTIONS = Set.of(
    JavaCore.COMPILER_PB_UNUSED_IMPORT,
    JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK,
    JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT,
    JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK);

  private static final Set<Integer> WRONGLY_CATEGORIZED_AS_SYNTAX_ERROR = Set.of(
    IProblem.SwitchExpressionsYieldMissingDefaultCase,
    IProblem.SwitchExpressionsYieldMissingEnumConstantCase);

  private final String message;
  private final ProblemKind type;

  JDTProblem(String message, ProblemKind type) {
    this.message = type == ProblemKind.PREVIEW_FEATURE_USED && message.endsWith(USE_ENABLE_PREVIEW)
      ? message.substring(0, message.length() - USE_ENABLE_PREVIEW.length())
      : message;
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
    return message();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JDTProblem that)) {
      return false;
    }
    return type == that.type && message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message);
  }

  public static Set<String> compilerOptions() {
    return COMPILER_OPTIONS;
  }

  @CheckForNull
  static ProblemKind kindOf(IProblem problem) {
    return PROBLEM_KINDS.get(problem.getID());
  }

  /**
   * Missing switch defaults can be caused by incomplete enum type information rather than invalid syntax.
   */
  static boolean isSyntaxError(IProblem problem) {
    return (problem.getID() & IProblem.Syntax) != 0 && !WRONGLY_CATEGORIZED_AS_SYNTAX_ERROR.contains(problem.getID());
  }

  static Set<Problem> semanticProblems(List<IProblem> errors) {
    return errors.stream()
      .filter(error -> (error.getID() & IProblem.UndefinedType) != 0)
      .map(error -> new JDTProblem(error.getMessage(), (error.getID() & IProblem.PreviewFeatureUsed) == 0 ? ProblemKind.UNDEFINED_TYPE : ProblemKind.PREVIEW_FEATURE_USED))
      .collect(Collectors.toSet());
  }
}
