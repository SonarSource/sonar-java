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
package org.sonar.java.regex.parsertests;

import org.junit.jupiter.api.Test;
import org.sonar.java.regex.ast.DotTree;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;

import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertCharacterClass;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertType;

class DotTests {

  @Test
  void onlyDot() {
    RegexTree regex = assertSuccessfulParse(".");
    assertType(DotTree.class, regex);
  }

  @Test
  void escapedDot() {
    assertPlainCharacter('.', "\\\\.");
  }

  @Test
  void dotInCharacterClass() {
    RegexTree regex = assertSuccessfulParse("[.]");
    assertPlainCharacter('.', assertCharacterClass(false, regex));
  }

  @Test
  void quantifiedDot() {
    RegexTree regex = assertSuccessfulParse(".*");
    assertType(DotTree.class, assertType(RepetitionTree.class, regex).getElement());
  }

}
