/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.reporting;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaQuickFixTest {

  @Test
  void test_build_quick_fix_without_edits() {
    JavaQuickFix quickFix = JavaQuickFix.newQuickFix("description").build();
    assertThat(quickFix.getDescription()).isEqualTo("description");
    assertThat(quickFix.getTextEdits()).isEmpty();
  }

  @Test
  void test_build_quick_fix_with_formated_mesasge() {
    JavaQuickFix quickFix = JavaQuickFix.newQuickFix("description %s %d", "yolo", 42).build();
    assertThat(quickFix.getDescription()).isEqualTo("description yolo 42");
    assertThat(quickFix.getTextEdits()).isEmpty();
  }

  @Test
  void test_build_quick_fix_with_edits() {
    JavaTextEdit edit = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(1,2,3,4));
    JavaQuickFix quickFix = JavaQuickFix.newQuickFix("description")
      .addTextEdits(Collections.singletonList(edit))
      .build();
    assertThat(quickFix.getDescription()).isEqualTo("description");
    assertThat(quickFix.getTextEdits()).hasSize(1).containsExactly(edit);
  }

  @Test
  void test_can_set_edits_multiples_times() {
    JavaTextEdit edit1 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(1,2,3,4));
    JavaTextEdit edit2 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(2,3,4,5));
    JavaQuickFix quickFix = JavaQuickFix.newQuickFix("description")
      .addTextEdits(Collections.singletonList(edit1))
      .addTextEdit(edit2)
      .build();
    assertThat(quickFix.getDescription()).isEqualTo("description");
    assertThat(quickFix.getTextEdits()).hasSize(2).containsExactly(edit1, edit2);
  }

  @Test
  void reverseSortEdits_sorts_as_expected() {
    JavaTextEdit edit1 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(1,2,3,4));
    JavaTextEdit edit2 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(2,3,4,5));

    assert_text_edits_are_ordered_as_expected(
      Collections.emptyList(),
      Collections.emptyList()
    );

    assert_text_edits_are_ordered_as_expected(
      Collections.singletonList(edit1),
      Collections.singletonList(edit1)
    );

    assert_text_edits_are_ordered_as_expected(
      List.of(edit1, edit1),
      List.of(edit1, edit1)
    );

    assert_text_edits_are_ordered_as_expected(
      List.of(edit1, edit2),
      List.of(edit2, edit1)
    );

    JavaTextEdit edit3 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(1, 1, 1, 2));
    JavaTextEdit edit4 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(1, 1, 2, 1));
    assert_text_edits_are_ordered_as_expected(
      List.of(edit3, edit4),
      List.of(edit4, edit3)
    );

    JavaTextEdit edit5 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(1, 1, 1, 1));
    JavaTextEdit edit6 = JavaTextEdit.removeTextSpan(JavaTextEdit.textSpan(1, 2, 1, 3));
    assert_text_edits_are_ordered_as_expected(
      List.of(edit5, edit6),
      List.of(edit6, edit5)
    );
  }

  void assert_text_edits_are_ordered_as_expected(List<JavaTextEdit> editsToAdd, List<JavaTextEdit> expectedOrder) {
    JavaQuickFix quickFix = JavaQuickFix.newQuickFix("Text edits should be ordered as expected")
      .addTextEdits(editsToAdd)
      .reverseSortEdits()
      .build();
    assertThat(quickFix.getTextEdits()).isEqualTo(expectedOrder);
  }

}
