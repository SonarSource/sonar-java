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

import static org.sonar.java.regex.RegexParserTestUtils.assertJavaCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertListElements;
import static org.sonar.java.regex.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.RegexParserTestUtils.assertType;

class DisjunctionTreeTest {

  @Test
  void disjunctionWithTwoAlternatives() {
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, assertSuccessfulParse("a|b"));
    assertListElements(disjunction.getAlternatives(),
      first -> assertPlainCharacter('a', first),
      second -> assertPlainCharacter('b', second)
    );
    assertListElements(disjunction.getOrOperators(),
      first -> assertJavaCharacter(1, '|', first)
    );
  }

  @Test
  void disjunctionWithThreeAlternatives() {
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, assertSuccessfulParse("a|b|c"));
    assertListElements(disjunction.getAlternatives(),
      first -> assertPlainCharacter('a', first),
      second -> assertPlainCharacter('b', second),
      third -> assertPlainCharacter('c', third)
    );
    assertListElements(disjunction.getOrOperators(),
      first -> assertJavaCharacter(1, '|', first),
      second -> assertJavaCharacter(3, '|', second)
    );
  }

  @Test
  void disjunctionWithQuoting() {
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, assertSuccessfulParse("\\\\Qa\\\\E|b"));
    assertListElements(disjunction.getAlternatives(),
      first -> assertPlainCharacter('a', first),
      second -> assertPlainCharacter('b', second)
    );
    assertListElements(disjunction.getOrOperators(),
      first -> assertJavaCharacter(7, '|', first)
    );
  }

  @Test
  void disjunctionWithQuoting2() {
    DisjunctionTree disjunction = assertType(DisjunctionTree.class, assertSuccessfulParse("a\\\\Q\\\\E|b\\\\Q\\\\E"));
    assertListElements(disjunction.getAlternatives(),
      first -> assertPlainCharacter('a', first),
      second -> assertPlainCharacter('b', second)
    );
    assertListElements(disjunction.getOrOperators(),
      first -> assertJavaCharacter(7, '|', first)
    );
  }

}
