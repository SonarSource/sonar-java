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
package org.sonar.java.checks.helpers;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValueBasedUtilsTest {

  @Test
  void testIsValueBased() throws Exception {
    File file = new File("src/test/files/checks/helpers/ValueBasedUtilsTest.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);

    List<Tree> members = ((ClassTree) tree.types().get(0)).members();
    members.stream()
      .forEach(member -> checkMember((VariableTree)member));
  }

  private static void checkMember(VariableTree member) {
    boolean expected = getCommentValue(member);
    boolean found = ValueBasedUtils.isValueBased(member.type().symbolType());
    assertThat(found).as("Wrong value for field '" + member.symbol().name() + "'").isEqualTo(expected);
  }

  private static boolean getCommentValue(VariableTree member) {
    SyntaxTrivia trivia = member.firstToken().trivias().get(0);
    String value = trivia.comment().substring(16);
    return Boolean.valueOf(value);
  }

}
