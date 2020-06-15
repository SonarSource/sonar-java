/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.regex.ast;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;

class BackReferenceTreeTest {

  @Test
  void backReferences() {
    assertBackReference("\\\\k<group1>", "group1");
    assertBackReference("\\\\k<ALPHA>", "ALPHA");
    assertBackReference("\\\\k<0invalid>", "0invalid");

    assertBackReference("\\\\1", 1);
    assertBackReference("\\\\42", 42);
    // octal
    assertBackReference("\\\\042", 34);

    RegexTree regex = assertSuccessfulParse("\\\\42.");
    assertThat(regex.is(RegexTree.Kind.SEQUENCE)).isTrue();
    SequenceTree seq = (SequenceTree) regex;
    assertThat(seq.getItems()).hasSize(2);
    assertThat(seq.getItems().get(0)).isInstanceOf(BackReferenceTree.class);
    assertThat(seq.getItems().get(1)).isInstanceOf(DotTree.class);

    RegexTree regex2 = assertSuccessfulParse("\\\\42a");
    assertThat(regex2.is(RegexTree.Kind.SEQUENCE)).isTrue();
    SequenceTree seq2 = (SequenceTree) regex2;
    assertThat(seq2.getItems()).hasSize(2);
    assertThat(seq2.getItems().get(0)).isInstanceOf(BackReferenceTree.class);
    assertThat(seq2.getItems().get(1)).isInstanceOf(PlainCharacterTree.class);
  }

  @Test
  void failingInvalidBackReferences() {
    assertFailParsing("\\\\ko", "Expected '<', but found 'o'");
    assertFailParsing("\\\\k<", "Expected a group name, but found the end of the regex");
    assertFailParsing("\\\\k<o", "Expected '>', but found the end of the regex");
    assertFailParsing("\\\\k<>", "Expected a group name, but found '>'");
  }

  private static void assertBackReference(String regex, String expectedGroupName) {
    RegexTree tree = assertSuccessfulParse(regex);
    assertThat(tree).isInstanceOf(BackReferenceTree.class);
    assertKind(RegexTree.Kind.BACK_REFERENCE, tree);

    BackReferenceTree backReferenceTree = (BackReferenceTree) tree;
    assertThat(backReferenceTree.isNamedGroup()).isTrue();
    assertThat(backReferenceTree.groupName()).isEqualTo(expectedGroupName);
    assertThat(backReferenceTree.groupNumber()).isEqualTo(-1);
    assertThat(backReferenceTree.isNumerical()).isFalse();
  }

  private static void assertBackReference(String regex, int expectedGroupNumber) {
    RegexTree tree = assertSuccessfulParse(regex);
    assertThat(tree).isInstanceOf(BackReferenceTree.class);

    BackReferenceTree backReferenceTree = (BackReferenceTree) tree;
    assertThat(backReferenceTree.isNumerical()).isTrue();
    assertThat(backReferenceTree.groupNumber()).isEqualTo(expectedGroupNumber);
    assertThat(backReferenceTree.isNamedGroup()).isFalse();
  }

}
