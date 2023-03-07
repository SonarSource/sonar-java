/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.reporting;

import java.util.Collections;
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

}
