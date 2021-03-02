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

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.RegexParserTestUtils.assertCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainString;
import static org.sonar.java.regex.RegexParserTestUtils.assertSingleEdge;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class SequenceTreeTest {

  @Test
  void emptyString() {
    RegexTree regex = assertSuccessfulParse("");
    assertLocation(0, 0, regex);
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    assertEquals(AutomatonState.TransitionType.EPSILON, regex.incomingTransitionType());
    assertSingleEdge(regex, regex.continuation(), AutomatonState.TransitionType.EPSILON);
  }

  @Test
  void multipleEscapes() {
    SequenceTree sequence = assertType(SequenceTree.class, assertSuccessfulParse("\\123\\124"));
    assertListElements(sequence.getItems(),
      first -> {
        assertCharacter('S', first);
        assertLocation(0, 4, first);
      },
      second -> {
        assertCharacter('T', second);
        assertLocation(4, 8, second);
      }
    );
    assertEquals(AutomatonState.TransitionType.EPSILON, sequence.incomingTransitionType());
    assertSingleEdge(sequence, sequence.getItems().get(0), AutomatonState.TransitionType.CHARACTER);
    assertSingleEdge(sequence.getItems().get(0), sequence.getItems().get(1), AutomatonState.TransitionType.CHARACTER);
    assertSingleEdge(sequence.getItems().get(1), sequence.continuation(), AutomatonState.TransitionType.EPSILON);
  }

  @Test
  void octalEscapeLimit() {
    SequenceTree sequence = assertType(SequenceTree.class, assertSuccessfulParse("\\456"));
    assertListElements(sequence.getItems(),
      first -> {
        assertCharacter('%', first);
        assertLocation(0, 3, first);
      },
      second -> {
        assertCharacter('6', second);
        assertLocation(3, 4, second);
      }
    );
    assertEquals(AutomatonState.TransitionType.EPSILON, sequence.incomingTransitionType());
    assertSingleEdge(sequence, sequence.getItems().get(0), AutomatonState.TransitionType.CHARACTER);
    assertSingleEdge(sequence.getItems().get(0), sequence.getItems().get(1), AutomatonState.TransitionType.CHARACTER);
    assertSingleEdge(sequence.getItems().get(1), sequence.continuation(), AutomatonState.TransitionType.EPSILON);
  }

  @Test
  void quotedString() {
    assertPlainString("a(b)\\w|cd*[]", "\\\\Qa(b)\\\\w|cd*[]\\\\E");
    assertPlainString("a(b)\\w|cd*[]", "a\\\\Q(b)\\\\w|\\\\Ecd\\\\Q*[]\\\\E");
    assertPlainString("a(b)\\w|cd*[]\\", "\\\\Qa(b)\\\\w|cd*[]\\\\\\\\E");
  }

  @Test
  void quotedStringWithComments() {
    assertPlainString("a#b", "\\\\Qa#b\\\\E", Pattern.COMMENTS);
    assertPlainString("ab", "ab#\\\\Qc\\\\E", Pattern.COMMENTS);
    assertPlainString("ab", "\\\\Qab\\\\E#\\\\Qb\\\\E", Pattern.COMMENTS);
    assertPlainString("", "\\\\Q\\\\E#lala", Pattern.COMMENTS);
    assertPlainString("a b", "\\\\Qa b\\\\E", Pattern.COMMENTS);
    assertPlainString("ab", "\\\\Qa\\\\E \\\\Qb\\\\E", Pattern.COMMENTS);
  }

  @Test
  void illegalQuotedString() {
    assertFailParsing("abc\\\\E", "\\E used without \\Q");
    assertFailParsing("\\\\Qabc", "Expected '\\E', but found the end of the regex");
  }

}
