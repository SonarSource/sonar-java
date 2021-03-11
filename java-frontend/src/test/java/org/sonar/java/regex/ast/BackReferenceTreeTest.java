/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang.StringEscapeUtils.escapeJava;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;

class BackReferenceTreeTest {

  @Test
  void backReferences() {
    assertBackReference(escapeJava("\\k<group1>"), "group1");
    assertBackReference(escapeJava("\\k<ALPHA>"), "ALPHA");
    assertBackReference(escapeJava("\\k<0invalid>"), "0invalid");

    assertBackReference(escapeJava("\\1"), 1);
    assertBackReference(escapeJava("(1)\\1"), 1);
    assertBackReference(escapeJava("(1)\\7"), 7);
    assertBackReference(escapeJava("(1)\\11"), 1);
    assertBackReference(escapeJava("(1)(2)(3)(4)(5)(6)(7)(8)(9)(a)\\11"), 1);
    assertBackReference(escapeJava("(1)(2)(3)(4)(5)(6)(7)(8)(9)(a)(b)\\11"), 11);
    assertBackReference(escapeJava("(1)(2)(3)(4)(5)(6)(7)(8)(9)(a)(b)(c)\\11"), 11);
    assertBackReference(escapeJava("(((((5)(6)(7)(8)(9)(a)\\11)?)+)*)"), 1);
    assertBackReference(escapeJava("(((((5)(6)(7)(8)(9)(a)(b)\\11)?)+)*)"), 11);

    RegexTree regex = assertSuccessfulParse(escapeJava("\\42."));
    assertThat(regex.is(RegexTree.Kind.SEQUENCE)).isTrue();
    SequenceTree seq = (SequenceTree) regex;
    assertThat(seq.getItems()).hasSize(3);
    assertThat(seq.getItems().get(0)).isInstanceOf(BackReferenceTree.class);
    assertThat(seq.getItems().get(1)).isInstanceOf(CharacterTree.class);
    assertThat(seq.getItems().get(2)).isInstanceOf(DotTree.class);

    RegexTree regex2 = assertSuccessfulParse(escapeJava("\\42a"));
    assertThat(regex2.is(RegexTree.Kind.SEQUENCE)).isTrue();
    SequenceTree seq2 = (SequenceTree) regex2;
    assertThat(seq2.getItems()).hasSize(3);
    assertThat(seq2.getItems().get(0)).isInstanceOf(BackReferenceTree.class);
    assertThat(seq2.getItems().get(1)).isInstanceOf(CharacterTree.class);
    assertThat(seq2.getItems().get(2)).isInstanceOf(CharacterTree.class);
  }

  @Test
  void backReferenceNumericGroup() {
    RegexTree regex = assertSuccessfulParse("(a)(b)\\\\k<foo>\\\\1\\\\2\\\\3");
    assertThat(regex.is(RegexTree.Kind.SEQUENCE)).isTrue();
    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertThat(items).hasSize(6);
    assertThat(items.get(0)).isInstanceOf(CapturingGroupTree.class);
    assertThat(items.get(1)).isInstanceOf(CapturingGroupTree.class);
    CapturingGroupTree capturingGroup1 = (CapturingGroupTree) items.get(0);
    CapturingGroupTree capturingGroup2 = (CapturingGroupTree) items.get(1);

    assertThat(((BackReferenceTree)items.get(2)).group()).isNull();
    assertThat(((BackReferenceTree)items.get(3)).group()).isEqualTo(capturingGroup1);
    assertThat(((BackReferenceTree)items.get(4)).group()).isEqualTo(capturingGroup2);
    assertThat(((BackReferenceTree)items.get(5)).group()).isNull();
  }

  @Test
  void backReferenceNameGroup() {
    RegexTree regex = assertSuccessfulParse("(?<foo>a)\\\\k<foo>\\\\k<bar>\\\\1\\\\2");
    assertThat(regex.is(RegexTree.Kind.SEQUENCE)).isTrue();
    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertThat(items).hasSize(5);
    assertThat(items.get(0)).isInstanceOf(CapturingGroupTree.class);
    CapturingGroupTree capturingGroup = (CapturingGroupTree) items.get(0);

    assertThat(((BackReferenceTree)items.get(1)).group()).isEqualTo(capturingGroup);
    assertThat(((BackReferenceTree)items.get(2)).group()).isNull();
    assertThat(((BackReferenceTree)items.get(3)).group()).isEqualTo(capturingGroup);
    assertThat(((BackReferenceTree)items.get(4)).group()).isNull();
  }

  @Test
  void failingInvalidBackReferences() {
    assertFailParsing(escapeJava("\\ko"), "Expected '<', but found 'o'");
    assertFailParsing(escapeJava("\\k<"), "Expected a group name, but found the end of the regex");
    assertFailParsing(escapeJava("\\k<o"), "Expected '>', but found the end of the regex");
    assertFailParsing(escapeJava("\\k<>"), "Expected a group name, but found '>'");
  }

  private static void assertBackReference(String regex, String expectedGroupName) {
    BackReferenceTree backReferenceTree = assertSuccessfulParseBackReference(regex);
    assertThat(backReferenceTree.incomingTransitionType()).isEqualTo(AutomatonState.TransitionType.BACK_REFERENCE);
    assertThat(backReferenceTree.isNamedGroup()).isTrue();
    assertThat(backReferenceTree.groupName()).isEqualTo(expectedGroupName);
    assertThat(backReferenceTree.groupNumber()).isEqualTo(-1);
    assertThat(backReferenceTree.isNumerical()).isFalse();
  }

  private static void assertBackReference(String regex, int expectedGroupNumber) {
    BackReferenceTree backReferenceTree = assertSuccessfulParseBackReference(regex);
    assertThat(backReferenceTree.incomingTransitionType()).isEqualTo(AutomatonState.TransitionType.BACK_REFERENCE);
    assertThat(backReferenceTree.isNumerical()).isTrue();
    assertThat(backReferenceTree.groupNumber()).isEqualTo(expectedGroupNumber);
    assertThat(backReferenceTree.isNamedGroup()).isFalse();
  }

  private static BackReferenceTree assertSuccessfulParseBackReference(String regex) {
    RegexTree tree = assertSuccessfulParse(regex);
    BackReferenceCollector backReferenceCollector = new BackReferenceCollector();
    tree.accept(backReferenceCollector);
    assertThat(backReferenceCollector.backReferences).hasSize(1);
    return backReferenceCollector.backReferences.get(0);
  }

  static class BackReferenceCollector extends RegexBaseVisitor {
    List<BackReferenceTree> backReferences = new ArrayList<>();

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      backReferences.add(tree);
      super.visitBackReference(tree);
    }
  }

}
