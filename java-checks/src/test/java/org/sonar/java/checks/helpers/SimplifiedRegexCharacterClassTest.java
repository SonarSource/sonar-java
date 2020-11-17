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
package org.sonar.java.checks.helpers;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.ast.CharacterClassElementTree;
import org.sonar.java.regex.ast.FlagSet;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.MiscEscapeSequenceTree;
import org.sonar.java.regex.ast.RegexSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimplifiedRegexCharacterClassTest {

  @Test
  void testIntersectionWithTrueAsDefaultAnswer() {
    RegexSource dummySource = new RegexSource(Collections.emptyList());
    IndexRange dummyRange = new IndexRange(0, 0);
    CharacterClassElementTree dummyTree = new MiscEscapeSequenceTree(dummySource, dummyRange);

    SimplifiedRegexCharacterClass aToZ = new SimplifiedRegexCharacterClass();
    aToZ.addRange('a', 'z', dummyTree);
    SimplifiedRegexCharacterClass unknown = new SimplifiedRegexCharacterClass();
    unknown.add(dummyTree, new FlagSet());
    SimplifiedRegexCharacterClass empty = new SimplifiedRegexCharacterClass();

    assertTrue(aToZ.intersects(unknown, true));
    assertFalse(aToZ.intersects(empty, true));
    assertFalse(unknown.intersects(empty, true));

    assertTrue(unknown.intersects(aToZ, true));
    assertFalse(empty.intersects(aToZ, true));
    assertFalse(empty.intersects(unknown, true));
  }
}
