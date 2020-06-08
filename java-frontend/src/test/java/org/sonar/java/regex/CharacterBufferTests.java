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

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.JavaCharacter;
import org.sonar.java.regex.ast.RegexSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterBufferTests {

  @Test
  void testEmpty() {
    assertEmptyBuffer(new CharacterBuffer(42));
    assertEmptyBuffer(new CharacterBuffer(1));
  }

  @Test
  void testAddingGettingAndRemoving() {
    CharacterBuffer buffer = new CharacterBuffer(2);
    buffer.add(makeCharacter('a'));
    buffer.add(makeCharacter('b'));
    assertEquals('a', buffer.get(0).getCharacter());
    assertEquals('b', buffer.get(1).getCharacter());
    buffer.removeFirst();
    assertEquals('b', buffer.get(0).getCharacter());
    buffer.add(makeCharacter('c'));
    buffer.add(makeCharacter('d'));
    assertEquals('b', buffer.get(0).getCharacter());
    assertEquals('c', buffer.get(1).getCharacter());
    assertEquals('d', buffer.get(2).getCharacter());
    buffer.removeFirst();
    assertEquals('c', buffer.get(0).getCharacter());
    assertEquals('d', buffer.get(1).getCharacter());
  }

  private void assertEmptyBuffer(CharacterBuffer buffer) {
    assertTrue(buffer.isEmpty(), "Empty buffer should be empty.");
    assertEquals(0, buffer.size(), "Size of empty buffer should be 0.");
    buffer.add(makeCharacter('x'));
    assertFalse(buffer.isEmpty(), "Non-empty buffer should be non-empty.");
    assertNotEquals(0, buffer.size(), "Non-empty buffer should not have size 0");
  }

  private JavaCharacter makeCharacter(char c) {
    return new JavaCharacter(new RegexSource(Collections.emptyList()), new IndexRange(0, 1), c);
  }
}
