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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JProblemTest {

  @Test
  void normalizes_preview_messages_at_construction() {
    String message = "Preview feature used Use --enable-preview to enable";
    JProblem preview = new JProblem(message, ProblemKind.PREVIEW_FEATURE_USED);
    assertThat(preview.message()).isEqualTo("Preview feature used");
    assertThat(preview).hasToString(preview.message());
    assertThat(new JProblem("Preview feature used", ProblemKind.PREVIEW_FEATURE_USED).message()).isEqualTo("Preview feature used");
    assertThat(new JProblem(message + ".", ProblemKind.PREVIEW_FEATURE_USED).message()).isEqualTo(message + ".");
    for (ProblemKind kind : ProblemKind.values()) {
      if (kind != ProblemKind.PREVIEW_FEATURE_USED) {
        assertThat(new JProblem(message, kind).message()).isEqualTo(message);
      }
    }
  }

  @Test
  void equivalent_normalized_messages_have_equal_values_and_hashes() {
    JProblem withGuidance = new JProblem("Preview feature used Use --enable-preview to enable", ProblemKind.PREVIEW_FEATURE_USED);
    JProblem withoutGuidance = new JProblem("Preview feature used", ProblemKind.PREVIEW_FEATURE_USED);
    assertThat(withGuidance).isEqualTo(withoutGuidance);
    assertThat(withoutGuidance).isEqualTo(withGuidance);
    assertThat(withGuidance).hasSameHashCodeAs(withoutGuidance);
    assertThat(new HashSet<>(List.of(withGuidance, withoutGuidance))).containsExactly(withoutGuidance);
  }

  @Test
  void compiler_options_enable_only_supported_warnings() {
    assertThat(JProblem.compilerOptions()).containsExactlyInAnyOrder(
      JavaCore.COMPILER_PB_UNUSED_IMPORT,
      JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK,
      JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT,
      JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK);
    Set<String> options = JProblem.compilerOptions();
    assertThatThrownBy(options::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void classifies_ecj_syntax_errors_and_semantic_problems() {
    IProblem syntax = problem(IProblem.ParsingError, "Syntax");
    IProblem toleratedSwitch = problem(IProblem.SwitchExpressionsYieldMissingDefaultCase, "Switch");
    IProblem undefined = problem(IProblem.UndefinedType, "Unknown type");
    IProblem preview = problem(IProblem.PreviewFeatureDisabled, "Preview");
    assertThat(JProblem.isSyntaxError(syntax)).isTrue();
    assertThat(JProblem.isSyntaxError(toleratedSwitch)).isFalse();
    assertThat(JProblem.isSyntaxError(undefined)).isFalse();
    assertThat(JProblem.semanticProblems(List.of(syntax, undefined, preview)))
      .containsExactlyInAnyOrder(new JProblem("Unknown type", ProblemKind.UNDEFINED_TYPE), new JProblem("Preview", ProblemKind.PREVIEW_FEATURE_USED));
    assertThat(JProblem.kindOf(syntax)).isNull();
  }

  @Test
  void test_equals() {
    JProblem p1 = new JProblem("message1", ProblemKind.UNDEFINED_TYPE);
    assertFalse(p1.equals(null));
    assertTrue(p1.equals(p1));

    JProblem p2 = new JProblem("message1", ProblemKind.UNDEFINED_TYPE);
    assertTrue(p1.equals(p2));
    p2 = new JProblem("message2", ProblemKind.UNDEFINED_TYPE);
    assertFalse(p1.equals(p2));
    p2 = new JProblem("message1", ProblemKind.PREVIEW_FEATURE_USED);
    assertFalse(p1.equals(p2));
  }

  @Test
  void test_hashcode() {
    assertEquals(
      new JProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new JProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new JProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new JProblem("b", ProblemKind.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new JProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new JProblem("a", ProblemKind.PREVIEW_FEATURE_USED).hashCode());
  }

  private static IProblem problem(int id, String message) {
    IProblem problem = mock(IProblem.class);
    when(problem.getID()).thenReturn(id);
    when(problem.getMessage()).thenReturn(message);
    return problem;
  }
}
