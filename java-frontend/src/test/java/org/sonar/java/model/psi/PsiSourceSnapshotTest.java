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
package org.sonar.java.model.psi;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PsiSourceSnapshotTest {

  private static final String BACKSLASH = Character.toString(92);

  @Test
  void unchanged_sources_use_identity_coordinates() {
    for (String source : List.of("", "class A {}", "String s = \"" + BACKSLASH.repeat(2) + "u0061\";", BACKSLASH + "x")) {
      var snapshot = PsiSourceSnapshot.from(source);
      assertThat(snapshot.psiText()).isSameAs(source);
      assertThat(snapshot.translated()).isSameAs(source);
      for (int i = 0; i <= source.length(); i++) {
        assertThat(snapshot.originalOffset(i)).isEqualTo(i);
      }
    }
  }

  @Test
  void translates_eligible_backslashes_using_result_provenance() {
    for (int count = 1; count <= 8; count++) {
      String source = BACKSLASH.repeat(count) + "u0061";
      String expected = count % 2 == 0 ? source : BACKSLASH.repeat(count - 1) + "a";
      assertThat(PsiSourceSnapshot.from(source).translated()).as(source).isEqualTo(expected);
    }
    assertTranslation(escape("005c") + escape("0061"), BACKSLASH + "a");
    assertTranslation(escape("005c") + BACKSLASH + "u0061", BACKSLASH + "a");
    assertTranslation(escape("005c") + BACKSLASH.repeat(2) + "u0061", BACKSLASH.repeat(2) + "a");
    assertTranslation(escape("005c") + BACKSLASH.repeat(3) + "u0061", BACKSLASH.repeat(4) + "u0061");
  }

  @Test
  void never_recursively_translates_a_generated_backslash() {
    var snapshot = PsiSourceSnapshot.from(escape("005c") + "u0061");
    assertThat(snapshot.translated()).isEqualTo(BACKSLASH + "u0061");
    assertThat(snapshot.psiText()).isEqualTo(escape("005c") + "u0061");
  }

  @Test
  void repeated_u_characters_and_surrogate_units_retain_original_boundaries() {
    var repeated = PsiSourceSnapshot.from(BACKSLASH + "uuuu0041");
    assertThat(repeated.translated()).isEqualTo("A");
    assertThat(repeated.originalOffset(1)).isEqualTo(9);
    var supplementary = PsiSourceSnapshot.from(escape("D801") + escape("dc00"));
    assertThat(supplementary.translated()).isEqualTo(new String(Character.toChars(0x10400)));
    assertThat(supplementary.originalOffset(1)).isEqualTo(6);
    assertThat(supplementary.originalOffset(2)).isEqualTo(12);
  }

  @Test
  void maps_contraction_and_backslash_protection_to_original_utf16_boundaries() {
    String source = "x" + escape("0061") + "b" + escape("005c") + "u000aZ";
    var snapshot = PsiSourceSnapshot.from(source);
    assertThat(snapshot.translated()).isEqualTo("xab" + BACKSLASH + "u000aZ");
    assertThat(snapshot.psiText()).isEqualTo("xab" + escape("005c") + "u000aZ");
    assertThat(snapshot.originalText(1, 2)).isEqualTo(escape("0061"));
    assertThat(snapshot.logicalText(1, 2)).isEqualTo("a");
    assertThat(snapshot.originalText(3, 9)).isEqualTo(escape("005c"));
    assertThat(snapshot.logicalText(3, 9)).isEqualTo(BACKSLASH);
    int previous = 0;
    for (int i = 0; i <= snapshot.psiText().length(); i++) {
      int offset = snapshot.originalOffset(i);
      assertThat(offset).isBetween(previous, source.length());
      previous = offset;
    }
    assertThat(previous).isEqualTo(source.length());
    assertThat(snapshot.originalText(0, snapshot.psiText().length())).isEqualTo(source);
    assertThat(snapshot.logicalText(0, snapshot.psiText().length())).isEqualTo(snapshot.translated());
  }

  @Test
  void rejects_malformed_eligible_escapes_with_original_offsets() {
    for (String suffix : List.of("u", "uu", "u123", "u12x4", "u１２３４")) {
      assertThatThrownBy(() -> PsiSourceSnapshot.from("abc" + BACKSLASH + suffix))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Malformed Java Unicode escape at original offset 3");
      String ineligible = BACKSLASH.repeat(2) + suffix;
      assertThat(PsiSourceSnapshot.from(ineligible).translated()).isEqualTo(ineligible);
    }
  }

  @Test
  void offset_and_slice_bounds_are_checked() {
    var snapshot = PsiSourceSnapshot.from(escape("0061"));
    assertThatThrownBy(() -> snapshot.originalOffset(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> snapshot.originalOffset(2)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> snapshot.originalText(1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> snapshot.logicalText(0, 2)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  private static void assertTranslation(String source, String expected) {
    assertThat(PsiSourceSnapshot.from(source).translated()).as(source).isEqualTo(expected);
  }

  private static String escape(String hex) {
    return BACKSLASH + "u" + hex;
  }
}
