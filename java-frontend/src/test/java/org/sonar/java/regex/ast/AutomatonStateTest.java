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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.RegexParseResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParseResult;

class AutomatonStateTest {

  @Test
  void active_flags() {
    String regex = "(?m:^)(?i:a)(?s:.)(?u:(\\\\d\\\\X))(?d:$)(?U:\\\\x{F6})(?x:[b](?=\\\\p{Lu})|\\\\1)";
    assertThat(allStates(assertSuccessfulParseResult(regex)).stream()
      .map(AutomatonStateTest::printClassAndFlags)
      .collect(Collectors.joining("\n")))
        .isEqualTo("" +
          "StartState\n" +
          "SequenceTree\n" +
          "NonCapturingGroupTree<flags: m:3>\n" +
          "BoundaryTree<flags: m:3>\n" +
          "NonCapturingGroupTree<flags: i:9>\n" +
          "CharacterTree 'a' <flags: i:9>\n" +
          "NonCapturingGroupTree<flags: s:15>\n" +
          "DotTree<flags: s:15>\n" +
          "NonCapturingGroupTree<flags: u:21>\n" +
          "CapturingGroupTree<flags: u:21>\n" +
          "SequenceTree<flags: u:21>\n" +
          "EscapedCharacterClassTree<flags: u:21>\n" +
          "MiscEscapeSequenceTree<flags: u:21>\n" +
          "EndOfCapturingGroupState<flags: u:21>\n" +
          "NonCapturingGroupTree<flags: d:34>\n" +
          "BoundaryTree<flags: d:34>\n" +
          "NonCapturingGroupTree<flags: U:40>\n" +
          "CharacterTree 'ö' <flags: U:40>\n" +
          "NonCapturingGroupTree<flags: x:52>\n" +
          "DisjunctionTree<flags: x:52>\n" +
          "SequenceTree<flags: x:52>\n" +
          "CharacterClassTree<flags: x:52>\n" +
          "LookAroundTree<flags: x:52>\n" +
          "EscapedCharacterClassTree<flags: x:52>\n" +
          "EndOfLookaroundState<flags: x:52>\n" +
          "FinalState\n" +
          "BackReferenceTree<flags: x:52>");
  }

  @Test
  void active_flags_scope() {
    String regex = "(?i)a(?u:b)|[c](?-i:d)(?u)e((?-U)f)g(?U)h(?-u)i";
    assertThat(allStates(assertSuccessfulParseResult(regex)).stream()
      .map(AutomatonStateTest::printClassAndFlags)
      .collect(Collectors.joining("\n")))
        .isEqualTo("" +
          "StartState\n" +
          "DisjunctionTree\n" +
          "SequenceTree\n" +
          "NonCapturingGroupTree<flags: i:3>\n" +
          "CharacterTree 'a' <flags: i:3>\n" +
          "NonCapturingGroupTree<flags: i:3 u:8>\n" +
          "CharacterTree 'b' <flags: i:3 u:8>\n" +
          "FinalState<flags: i:3 U:39>\n" +
          "SequenceTree<flags: i:3>\n" +
          "CharacterClassTree<flags: i:3>\n" +
          "NonCapturingGroupTree\n" +
          "CharacterTree 'd' \n" +
          "NonCapturingGroupTree<flags: i:3 u:25>\n" +
          "CharacterTree 'e' <flags: i:3 u:25>\n" +
          "CapturingGroupTree<flags: i:3 u:25>\n" +
          "SequenceTree<flags: i:3 u:25>\n" +
          "NonCapturingGroupTree<flags: i:3>\n" +
          "CharacterTree 'f' <flags: i:3>\n" +
          "EndOfCapturingGroupState<flags: i:3 u:25>\n" +
          "CharacterTree 'g' <flags: i:3 u:25>\n" +
          "NonCapturingGroupTree<flags: i:3 u:25 U:39>\n" +
          "CharacterTree 'h' <flags: i:3 u:25 U:39>\n" +
          "NonCapturingGroupTree<flags: i:3 U:39>\n" +
          "CharacterTree 'i' <flags: i:3 U:39>");
  }

  @Test
  void active_flags_scope_with_different_types_of_groups() {
    String regex = "(?i)a(?:(?u)b)|[c](?>(?-i)d)(?u)e(?=(?-U)f)g(?U)h(?-u)i";
    assertThat(allStates(assertSuccessfulParseResult(regex)).stream()
      .map(AutomatonStateTest::printClassAndFlags)
      .collect(Collectors.joining("\n")))
        .isEqualTo("" +
          "StartState\n" +
          "DisjunctionTree\n" +
          "SequenceTree\n" +
          "NonCapturingGroupTree<flags: i:3>\n" +
          "CharacterTree 'a' <flags: i:3>\n" +
          "NonCapturingGroupTree<flags: i:3>\n" +
          "SequenceTree<flags: i:3>\n" +
          "NonCapturingGroupTree<flags: i:3 u:11>\n" +
          "CharacterTree 'b' <flags: i:3 u:11>\n" +
          "FinalState<flags: i:3 U:47>\n" +
          "SequenceTree<flags: i:3>\n" +
          "CharacterClassTree<flags: i:3>\n" +
          "AtomicGroupTree<flags: i:3>\n" +
          "SequenceTree<flags: i:3>\n" +
          "NonCapturingGroupTree\n" +
          "CharacterTree 'd' \n" +
          "NonCapturingGroupTree<flags: i:3 u:31>\n" +
          "CharacterTree 'e' <flags: i:3 u:31>\n" +
          "LookAroundTree<flags: i:3 u:31>\n" +
          "SequenceTree<flags: i:3 u:31>\n" +
          "NonCapturingGroupTree<flags: i:3>\n" +
          "CharacterTree 'f' <flags: i:3>\n" +
          "EndOfLookaroundState<flags: i:3 u:31>\n" +
          "CharacterTree 'g' <flags: i:3 u:31>\n" +
          "NonCapturingGroupTree<flags: i:3 u:31 U:47>\n" +
          "CharacterTree 'h' <flags: i:3 u:31 U:47>\n" +
          "NonCapturingGroupTree<flags: i:3 U:47>\n" +
          "CharacterTree 'i' <flags: i:3 U:47>");
  }

  private static String printClassAndFlags(AutomatonState state) {
    String content = (state instanceof CharacterTree) ? " '" + ((CharacterTree) state).characterAsString() + "' " : "";
    return state.getClass().getSimpleName() + content + printFlags(state.activeFlags());
  }

  private static String printFlags(FlagSet flags) {
    StringBuilder out = new StringBuilder();
    if (!flags.isEmpty()) {
      out.append("<flags:");
      for (int i = 1; i <= flags.getMask(); i <<= 1) {
        SourceCharacter characterForFlag = flags.getJavaCharacterForFlag(i);
        if (characterForFlag != null) {
          out
            .append(" ")
            .append(characterForFlag.getCharacter())
            .append(":").append(characterForFlag.getRange().getBeginningOffset() + 1);
        }
      }
      out.append(">");
    }
    return out.toString();
  }

  private Collection<AutomatonState> allStates(RegexParseResult result) {
    Collection<AutomatonState> all = new LinkedHashSet<>();
    appendStates(all, result.getStartState());
    return all;
  }

  private void appendStates(Collection<AutomatonState> all, AutomatonState state) {
    if (all.add(state)) {
      state.successors().forEach(successor -> appendStates(all, successor));
    }
  }

}
