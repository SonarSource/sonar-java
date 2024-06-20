/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;

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
      .allMatch(tree -> tree.is(RegexTree.Kind.CHARACTER));

    assertThat(correspondingTextSpans(regex)).hasSize(2);

    CharacterTree char1 = (CharacterTree) items.get(0);

    // empty filtered out
    assertThat(correspondingTextSpans(char1)).hasSize(1);

    CharacterTree char2 = (CharacterTree) items.get(1);
    assertThat(correspondingTextSpans(char2)).hasSize(1);
  }

  @Test
  void testTextSpansBetweenRegexSyntaxElement() {
    RegexTree regex = assertSuccessfulParse("ABCD");
    assertKind(RegexTree.Kind.SEQUENCE, regex);

    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertThat(items).hasSize(4);

    RegexTree regexTreeA = items.get(0);
    RegexTree regexTreeB = items.get(1);
    RegexTree regexTreeC = items.get(2);
    RegexTree regexTreeD = items.get(3);

    List<TextSpan> textSpans = correspondingTextSpans(Arrays.asList(regexTreeA,regexTreeB));
    assertThat(textSpans).hasSize(1);
    TextSpan aBTextSpan = textSpans.get(0);

    assertThat(aBTextSpan.startCharacter).isEqualTo(1);
    assertThat(aBTextSpan.endCharacter).isEqualTo(3);

    List<TextSpan> textSpans2 = correspondingTextSpans(Arrays.asList(regexTreeB, regexTreeC, regexTreeD));
    assertThat(textSpans2).hasSize(1);
    TextSpan bCDTextSpan = textSpans2.get(0);

    assertThat(bCDTextSpan.startCharacter).isEqualTo(2);
    assertThat(bCDTextSpan.endCharacter).isEqualTo(5);

    List<TextSpan> textSpans3 = correspondingTextSpans(Arrays.asList(regexTreeB, regexTreeD));
    assertThat(textSpans3).hasSize(2);
    TextSpan bTestSpan = textSpans3.get(0);
    TextSpan dTestSpan = textSpans3.get(1);

    assertThat(bTestSpan.startCharacter).isEqualTo(2);
    assertThat(bTestSpan.endCharacter).isEqualTo(3);
    assertThat(dTestSpan.startCharacter).isEqualTo(4);
    assertThat(dTestSpan.endCharacter).isEqualTo(5);

  }

  @Test
  void emptyRegex() {
    RegexTree regex = assertSuccessfulParse("");
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    assertThat(((SequenceTree) regex).getItems()).isEmpty();

    assertThat(correspondingTextSpans(regex)).hasSize(1);
  }

  @Test
  void one_location_to_single_location_items() {
    // issue with 1 location
    RegexTree regexTree = assertSuccessfulParse("A");
    RegexCheck.RegexIssueLocation issue = new RegexCheck.RegexIssueLocation(regexTree, "My issue message.");

    assertThat(issue.message()).isEqualTo("My issue message.");
    List<RegexIssueLocation> locations = issue.toSingleLocationItems();
    assertThat(locations).hasSize(1);

    assertThat(locations.get(0).message()).isEqualTo("My issue message.");
    assertThat(locations.get(0).locations()).hasSize(1);
    assertThat(locations.get(0).locations().get(0)).isSameAs(issue.locations().get(0));
  }

  @Test
  void three_locations_to_single_location_items() {
    // issue with 3 locations
    RegexTree regexTree = assertSuccessfulParse("A\" + \n  \"B\" + \n  \"C");
    RegexCheck.RegexIssueLocation issue = new RegexCheck.RegexIssueLocation(regexTree, "My issue message.");

    assertThat(issue.message()).isEqualTo("My issue message.");
    List<RegexIssueLocation> locations = issue.toSingleLocationItems();
    assertThat(locations).hasSize(3);

    assertThat(locations.get(0).message()).isEqualTo("My issue message.");
    assertThat(locations.get(0).locations()).hasSize(1);
    assertThat(locations.get(0).locations().get(0)).isSameAs(issue.locations().get(0));

    assertThat(locations.get(1).message()).isEqualTo("Continuing here");
    assertThat(locations.get(1).locations()).hasSize(1);
    assertThat(locations.get(1).locations().get(0)).isSameAs(issue.locations().get(1));

    assertThat(locations.get(2).message()).isEqualTo("Continuing here");
    assertThat(locations.get(2).locations()).hasSize(1);
    assertThat(locations.get(2).locations().get(0)).isSameAs(issue.locations().get(2));
  }

  @Test
  void test_from_commons_regex_regex_issue_location() {
    RegexTree regexTree = assertSuccessfulParse("A");
    org.sonarsource.analyzer.commons.regex.RegexIssueLocation regexIssueLocation =
      new org.sonarsource.analyzer.commons.regex.RegexIssueLocation(regexTree, "My issue message.");
    RegexCheck.RegexIssueLocation issue = RegexCheck.RegexIssueLocation.fromCommonsRegexIssueLocation(regexIssueLocation);

    assertThat(issue.message()).isEqualTo("My issue message.");
    List<RegexIssueLocation> locations = issue.toSingleLocationItems();
    assertThat(locations).hasSize(1);

    assertThat(locations.get(0).message()).isEqualTo("My issue message.");
    assertThat(locations.get(0).locations()).hasSize(1);
    assertThat(locations.get(0).locations().get(0)).isSameAs(issue.locations().get(0));
  }

  private static List<TextSpan> correspondingTextSpans(RegexTree tree) {
    return new RegexCheck.RegexIssueLocation(tree, "message").locations();
  }

  private static List<TextSpan> correspondingTextSpans(List<RegexSyntaxElement> trees) {
    return new RegexCheck.RegexIssueLocation(trees, "message").locations();
  }

}
