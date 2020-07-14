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

import static org.sonar.java.regex.RegexParserTestUtils.assertFailParsing;
import static org.sonar.java.regex.RegexParserTestUtils.assertKind;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainString;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class SequenceTreeTest {

  @Test
  void emptyString() {
    RegexTree regex = assertSuccessfulParse("");
    assertLocation(0, 0, regex);
    assertKind(RegexTree.Kind.SEQUENCE, regex);
  }

  @Test
  void multipleEscapes() {
    SequenceTree sequence = assertType(SequenceTree.class, assertSuccessfulParse("\\123\\124"));
    assertListElements(sequence.getItems(),
      first -> {
        assertPlainCharacter('S', first);
        assertLocation(0, 4, first);
      },
      second -> {
        assertPlainCharacter('T', second);
        assertLocation(4, 8, second);
      }
    );
  }

  @Test
  void octalEscapeLimit() {
    SequenceTree sequence = assertType(SequenceTree.class, assertSuccessfulParse("\\456"));
    assertListElements(sequence.getItems(),
      first -> {
        assertPlainCharacter('%', first);
        assertLocation(0, 3, first);
      },
      second -> {
        assertPlainCharacter('6', second);
        assertLocation(3, 4, second);
      }
    );
  }

  @Test
  void quotedString() {
    assertPlainString("a(b)\\w|cd*[]", "\\\\Qa(b)\\\\w|cd*[]\\\\E");
    assertPlainString("a(b)\\w|cd*[]", "a\\\\Q(b)\\\\w|\\\\Ecd\\\\Q*[]\\\\E");
    assertPlainString("a(b)\\w|cd*[]\\", "\\\\Qa(b)\\\\w|cd*[]\\\\\\\\E");
  }

  @Test
  void quotedStringWithComments() {
    assertPlainString("a#b", "\\\\Qa#b\\\\E", true);
    assertPlainString("ab", "ab#\\\\Qc\\\\E", true);
    assertPlainString("ab", "\\\\Qab\\\\E#\\\\Qb\\\\E", true);
    assertPlainString("", "\\\\Q\\\\E#lala", true);
    assertPlainString("a b", "\\\\Qa b\\\\E", true);
    assertPlainString("ab", "\\\\Qa\\\\E \\\\Qb\\\\E", true);
  }

  @Test
  void illegalQuotedString() {
    assertFailParsing("abc\\\\E", "\\E used without \\Q");
    assertFailParsing("\\\\Qabc", "Expected '\\E', but found the end of the regex");
  }

}
