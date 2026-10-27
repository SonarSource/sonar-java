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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

final class EcjDiagnosticProvider {

  private static final Map<JProblem.Type, Integer> IDS = Map.of(
    JProblem.Type.UNDEFINED_TYPE, IProblem.UndefinedType,
    JProblem.Type.PREVIEW_FEATURE_USED, IProblem.PreviewFeatureUsed,
    JProblem.Type.UNUSED_IMPORT, IProblem.UnusedImport,
    JProblem.Type.REDUNDANT_CAST, IProblem.UnnecessaryCast,
    JProblem.Type.ASSIGNMENT_HAS_NO_EFFECT, IProblem.AssignmentHasNoEffect,
    JProblem.Type.MASKED_CATCH, IProblem.MaskedCatch);

  private EcjDiagnosticProvider() {
  }

  static Set<String> compilerOptions() {
    return Set.of(
      JavaCore.COMPILER_PB_UNUSED_IMPORT,
      JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK,
      JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT,
      JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK
    );
  }

  static List<JWarning> warnings(CompilationUnit ast, LineColumnConverter positions) {
    return Stream.of(ast.getProblems())
      .map(problem -> convert(problem, positions))
      .filter(Objects::nonNull)
      .toList();
  }

  private static JWarning convert(IProblem problem, LineColumnConverter positions) {
    for (JProblem.Type type : JProblem.Type.values()) {
      if (problem.getID() == IDS.get(type)) {
        LineColumnConverter.Pos start = positions.toPos(problem.getSourceStart());
        LineColumnConverter.Pos end = positions.toPos(problem.getSourceStart());
        return new JWarning(problem.getMessage(), type, start.line(), start.columnOffset(), end.line(), end.columnOffset());
      }
    }
    return null;
  }
}
