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

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagSetTest {

  @Test
  void emptySet() {
    FlagSet empty = new FlagSet();
    assertTrue(empty.isEmpty());
    assertFalse(empty.contains(Pattern.CASE_INSENSITIVE));
    assertFalse(empty.contains(Pattern.COMMENTS));
    assertFalse(empty.contains(Pattern.MULTILINE));
    assertEquals(0, empty.getMask());
  }

  @Test
  void nonEmptySet() {
    FlagSet nonEmpty = new FlagSet(Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
    assertFalse(nonEmpty.isEmpty());
    assertTrue(nonEmpty.contains(Pattern.CASE_INSENSITIVE));
    assertTrue(nonEmpty.contains(Pattern.COMMENTS));
    assertFalse(nonEmpty.contains(Pattern.MULTILINE));
    assertEquals(Pattern.CASE_INSENSITIVE | Pattern.COMMENTS, nonEmpty.getMask());
  }

}
