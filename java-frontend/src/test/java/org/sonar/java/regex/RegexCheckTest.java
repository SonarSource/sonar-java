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
package org.sonar.java.regex;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.regex.ast.Location;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.SequenceTree;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;

class RegexCheckTest implements RegexCheck {

  @Test
  void regexLocationsToIssueLocations() {
    // force a separation
    RegexTree regex = assertSuccessfulParse("A\" + \n  \"B");
    assertKind(RegexTree.Kind.SEQUENCE, regex);

    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertThat(items)
      .hasSize(2)
      .allMatch(tree -> tree.is(RegexTree.Kind.PLAIN_CHARACTER));
    assertThat(regex.getLocations())
      .hasSize(2)
      .allMatch(loc -> !loc.isEmpty());

    assertThat(correspondingTextSpans(regex)).hasSize(2);

    PlainCharacterTree char1 = (PlainCharacterTree) items.get(0);
    List<Location> char1Locs = char1.getLocations();
    assertThat(char1Locs).hasSize(2);
    assertThat(char1Locs.get(0).isEmpty()).isFalse();
    assertThat(char1Locs.get(1).isEmpty()).isTrue();

    // empty filtered out
    assertThat(correspondingTextSpans(char1)).hasSize(1);

    PlainCharacterTree char2 = (PlainCharacterTree) items.get(1);
    List<Location> char2Locs = char2.getLocations();
    assertThat(char2Locs)
      .hasSize(1)
      .allMatch(loc -> !loc.isEmpty());
    assertThat(correspondingTextSpans(char2)).hasSize(1);
  }

  @Test
  void emptyRegex() {
    RegexTree regex = assertSuccessfulParse("");
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    assertThat(((SequenceTree) regex).getItems()).isEmpty();

    assertThat(correspondingTextSpans(regex)).hasSize(1);
  }

  private static List<TextSpan> correspondingTextSpans(RegexTree tree) {
    return new RegexCheck.RegexIssueLocation(tree, "message").locations();
  }

}
