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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JDTProblemTest {

  @Test
  void normalizes_preview_messages_at_construction() {
    String message = "Preview feature used Use --enable-preview to enable";
    JDTProblem preview = new JDTProblem(message, ProblemKind.PREVIEW_FEATURE_USED);
    assertThat(preview.message()).isEqualTo("Preview feature used");
    assertThat(preview).hasToString(preview.message());
    assertThat(new JDTProblem("Preview feature used", ProblemKind.PREVIEW_FEATURE_USED).message()).isEqualTo("Preview feature used");
    assertThat(new JDTProblem(message + ".", ProblemKind.PREVIEW_FEATURE_USED).message()).isEqualTo(message + ".");
    for (ProblemKind kind : ProblemKind.values()) {
      if (kind != ProblemKind.PREVIEW_FEATURE_USED) {
        assertThat(new JDTProblem(message, kind).message()).isEqualTo(message);
      }
    }
  }

  @Test
  void equivalent_normalized_messages_have_equal_values_and_hashes() {
    JDTProblem withGuidance = new JDTProblem("Preview feature used Use --enable-preview to enable", ProblemKind.PREVIEW_FEATURE_USED);
    JDTProblem withoutGuidance = new JDTProblem("Preview feature used", ProblemKind.PREVIEW_FEATURE_USED);
    assertThat(withGuidance).isEqualTo(withoutGuidance);
    assertThat(withoutGuidance).isEqualTo(withGuidance);
    assertThat(withGuidance).hasSameHashCodeAs(withoutGuidance);
    assertThat(new HashSet<>(List.of(withGuidance, withoutGuidance))).containsExactly(withoutGuidance);
  }

  @Test
  void compiler_options_enable_only_supported_warnings() {
    assertThat(JDTProblem.compilerOptions()).containsExactlyInAnyOrder(
      JavaCore.COMPILER_PB_UNUSED_IMPORT,
      JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK,
      JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT,
      JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK);
    Set<String> options = JDTProblem.compilerOptions();
    assertThatThrownBy(options::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void classifies_ecj_syntax_errors_and_semantic_problems() {
    IProblem syntax = problem(IProblem.ParsingError, "Syntax");
    IProblem toleratedSwitch = problem(IProblem.SwitchExpressionsYieldMissingDefaultCase, "Switch");
    IProblem undefined = problem(IProblem.UndefinedType, "Unknown type");
    IProblem preview = problem(IProblem.PreviewFeatureDisabled, "Preview");
    assertThat(JDTProblem.isSyntaxError(syntax)).isTrue();
    assertThat(JDTProblem.isSyntaxError(toleratedSwitch)).isFalse();
    assertThat(JDTProblem.isSyntaxError(undefined)).isFalse();
    assertThat(JDTProblem.semanticProblems(List.of(syntax, undefined, preview)))
      .containsExactlyInAnyOrder(new JDTProblem("Unknown type", ProblemKind.UNDEFINED_TYPE), new JDTProblem("Preview", ProblemKind.PREVIEW_FEATURE_USED));
    assertThat(JDTProblem.kindOf(syntax)).isNull();
  }

  @Test
  void test_equals() {
    JDTProblem p1 = new JDTProblem("message1", ProblemKind.UNDEFINED_TYPE);
    assertNotNull(p1);
    assertEquals(p1, p1);

    JDTProblem p2 = new JDTProblem("message1", ProblemKind.UNDEFINED_TYPE);
    assertEquals(p1, p2);
    p2 = new JDTProblem("message2", ProblemKind.UNDEFINED_TYPE);
    assertNotEquals(p1, p2);
    p2 = new JDTProblem("message1", ProblemKind.PREVIEW_FEATURE_USED);
    assertNotEquals(p1, p2);
  }

  @Test
  void test_hashcode() {
    assertEquals(
      new JDTProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new JDTProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new JDTProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new JDTProblem("b", ProblemKind.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new JDTProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new JDTProblem("a", ProblemKind.PREVIEW_FEATURE_USED).hashCode());
  }

  private static IProblem problem(int id, String message) {
    IProblem problem = mock(IProblem.class);
    when(problem.getID()).thenReturn(id);
    when(problem.getMessage()).thenReturn(message);
    return problem;
  }
}
