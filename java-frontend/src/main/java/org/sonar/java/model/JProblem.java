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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sonar.plugins.java.api.diagnostics.Problem;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.tree.Tree;

public class JProblem implements Problem {

  private static final String USE_ENABLE_PREVIEW = " Use --enable-preview to enable";
  private static final Map<ProblemKind, Set<Tree.Kind>> KINDS = Map.of(
    ProblemKind.UNUSED_IMPORT, Set.of(Tree.Kind.IMPORT),
    ProblemKind.REDUNDANT_CAST, Set.of(Tree.Kind.TYPE_CAST, Tree.Kind.PARENTHESIZED_EXPRESSION),
    ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, Set.of(Tree.Kind.ASSIGNMENT),
    ProblemKind.MASKED_CATCH, Set.of(Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT));
  private static final Set<String> COMPILER_OPTIONS = Set.of(
    "org.eclipse.jdt.core.compiler.problem.unusedImport",
    "org.eclipse.jdt.core.compiler.problem.unnecessaryTypeCheck",
    "org.eclipse.jdt.core.compiler.problem.noEffectAssignment",
    "org.eclipse.jdt.core.compiler.problem.hiddenCatchBlock");

  private final String message;
  private final ProblemKind type;

  JProblem(String message, ProblemKind type) {
    this.type = type;
    this.message = type == ProblemKind.PREVIEW_FEATURE_USED ? cleanMessage(message) : message;
  }

  @Override
  public ProblemKind type() {
    return type;
  }

  static Set<Tree.Kind> kinds(ProblemKind type) {
    return KINDS.getOrDefault(type, Set.of());
  }

  public static Set<String> compilerOptions() {
    return COMPILER_OPTIONS;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public String toString() {
    return message;
  }

  private static String cleanMessage(String message) {
    if (message.endsWith(USE_ENABLE_PREVIEW)) {
      // Using --enable-preview is irrelevant in this context.
      return message.substring(0, message.length() - USE_ENABLE_PREVIEW.length());
    }
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JProblem)) {
      return false;
    }
    JProblem that = (JProblem) o;
    return type == that.type && message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message);
  }

}
