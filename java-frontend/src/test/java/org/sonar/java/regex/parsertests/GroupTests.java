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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.RegexParser;
import org.sonar.java.regex.SyntaxError;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.Location;
import org.sonar.java.regex.ast.RegexTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertLocation;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertPlainCharacter;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.assertType;
import static org.sonar.java.regex.parsertests.RegexParserTestUtils.makeSource;

class GroupTests {

  @Test
  void testGroup() {
    RegexTree regex = assertSuccessfulParse("(x)");
    CapturingGroupTree group = assertType(CapturingGroupTree.class, regex);
    assertPlainCharacter('x', group.getElement());
    assertLocation(0, 3, group);
    assertLocation(1, 2, group.getElement());
  }

  @Test
  void testUnfinishedGroup() {
    RegexParseResult result = new RegexParser(makeSource("(x"), false).parse();
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Expected ')', but found the end of the regex", error.getMessage(), "Error should have the right message.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(2,2), locations.get(0).getIndexRange(), "Error should have the right location.");
    assertTrue(locations.get(0).getIndexRange().isEmpty(), "Error location should be empty range.");
  }

  @Test
  void testExtraParenthesis() {
    RegexParseResult result = new RegexParser(makeSource("(x))"), false).parse();
    assertEquals(1, result.getSyntaxErrors().size(), "Expected exactly one error.");
    SyntaxError error = result.getSyntaxErrors().get(0);
    assertEquals("Unexpected ')'", error.getMessage(), "Error should have the right message.");
    List<Location> locations = error.getLocations();
    assertEquals(1, locations.size(), "Error should only have one location.");
    assertEquals(new IndexRange(3,4), locations.get(0).getIndexRange(), "Error should have the right location.");
    assertFalse(locations.get(0).getIndexRange().isEmpty(), "Error location should not be empty range.");
  }

}
