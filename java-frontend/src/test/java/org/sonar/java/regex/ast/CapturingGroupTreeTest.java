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

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;

class CapturingGroupTreeTest {

  @Test
  void numbering() {
    RegexTree tree = assertSuccessfulParse("((A)(?:N)(B(?<groupC>C)))");
    assertKind(RegexTree.Kind.CAPTURING_GROUP, tree);
    CapturingGroupTree abc = ((CapturingGroupTree) tree);
    assertThat(abc.getGroupNumber()).isEqualTo(1);
    assertThat(abc.getName()).isEmpty();

    RegexTree abcElement = abc.getElement();
    assertKind(RegexTree.Kind.SEQUENCE, abcElement);
    List<RegexTree> abcItems = ((SequenceTree) abcElement).getItems();
    assertThat(abcItems).hasSize(3);
    assertThat(abcItems.stream().map(RegexTree::kind)).containsExactly(RegexTree.Kind.CAPTURING_GROUP, RegexTree.Kind.NON_CAPTURING_GROUP, RegexTree.Kind.CAPTURING_GROUP);

    CapturingGroupTree a = ((CapturingGroupTree) abcItems.get(0));
    CapturingGroupTree bc = ((CapturingGroupTree) abcItems.get(2));

    assertThat(a.getGroupNumber()).isEqualTo(2);
    assertThat(a.getName()).isEmpty();

    assertThat(bc.getGroupNumber()).isEqualTo(3);
    assertThat(bc.getName()).isEmpty();

    RegexTree bcElement = bc.getElement();
    assertKind(RegexTree.Kind.SEQUENCE, bcElement);
    List<RegexTree> bcItems = ((SequenceTree) bcElement).getItems();
    assertThat(bcItems).hasSize(2);

    assertKind(RegexTree.Kind.PLAIN_CHARACTER, bcItems.get(0));
    assertKind(RegexTree.Kind.CAPTURING_GROUP, bcItems.get(1));

    CapturingGroupTree c = ((CapturingGroupTree) bcItems.get(1));
    assertThat(c.getGroupNumber()).isEqualTo(4);
    assertThat(c.getName()).hasValue("groupC");
  }

}
