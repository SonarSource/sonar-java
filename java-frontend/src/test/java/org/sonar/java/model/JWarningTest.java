/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model;

import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.java.model.JWarning.Mapper.isInsideTree;
import static org.sonar.java.model.JWarning.Mapper.setSyntaxTree;
import static org.sonar.java.model.JWarning.Mapper.matchesTreeExactly;
import static org.sonar.java.model.JWarning.Mapper.isMorePreciseTree;

class JWarningTest {

  @Test
  void getters() {
    String message = "message";
    JWarning.Type type = JWarning.Type.UNUSED_IMPORT;
    int startLine = 0;
    int startColumn = 1;
    int endLine = 2;
    int endColumn = 3;
    JWarning warning = new JWarning(message, type, startLine, startColumn, endLine, endColumn);

    assertThat(warning.message()).isEqualTo(message);
    assertThat(warning.type()).isEqualTo(type);
    assertThat(warning.syntaxTree()).isNull();
  }

  @Test
  void test_equals() {
    JWarning j1 = new JWarning("a", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3);
    assertFalse(j1.equals(null));
    assertTrue(j1.equals(j1));

    JWarning j2 = new JWarning("a", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3);
    assertTrue(j1.equals(j2));

    // syntaxTree is ignored in JWarning.equals()
    Tree beforeDifferentLine = importTree(4, 0, 5, 19);
    setSyntaxTree(j2, beforeDifferentLine);
    assertTrue(j1.equals(j2));

    j2 = new JWarning("b", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3);
    assertFalse(j1.equals(j2));
    j2 = new JWarning("a", JProblem.Type.UNUSED_IMPORT, 42, 1, 2, 3);
    assertFalse(j1.equals(j2));
    j2 = new JWarning("a", JProblem.Type.UNUSED_IMPORT, 0, 1, 42, 3);
    assertFalse(j1.equals(j2));
  }

  @Test
  void test_hashcode() {
    JWarning j1 = new JWarning("a", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3);
    JWarning j2 = new JWarning("a", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3);
    assertEquals(j1.hashCode(), j2.hashCode());

    // syntaxTree is ignored in JWarning.hashCode()
    Tree beforeDifferentLine = importTree(4, 0, 5, 19);
    setSyntaxTree(j2, beforeDifferentLine);
    assertEquals(j1.hashCode(), j2.hashCode());

    assertNotEquals(
      new JWarning("a", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3).hashCode(),
      new JWarning("b", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3).hashCode());
    assertNotEquals(
      new JWarning("a", JProblem.Type.UNUSED_IMPORT, 0, 1, 2, 3).hashCode(),
      new JWarning("a", JProblem.Type.REDUNDANT_CAST, 0, 1, 2, 3).hashCode());
  }

  @Nested
  class MapperTest {
    private final VariableTree variable = variableTree(1, 10, 1, 20);
    private final IdentifierTree name = variable.simpleName();
    private final TypeTree type = variable.type();

    @Test
    void inside_multiline_warning() {
      JWarning warning = new JWarning("message", JWarning.Type.UNUSED_IMPORT, 5, 20, 7, 50);

      Tree beforeDifferentLine = importTree(4, 0, 5, 19);
      Tree beforeSameStartLine = importTree(5, 15, 6, 20);
      Tree includingSameStartLine = importTree(5, 15, 8, 15);
      Tree including = importTree(4, 15, 8, 55);
      Tree includingSameEndLine = importTree(5, 20, 7, 55);
      Tree afterSameEndLine = importTree(6, 15, 7, 55);
      Tree afterDifferentLine = importTree(8, 0, 9, 10);

      assertThat(isInsideTree(warning, beforeDifferentLine)).isFalse();
      assertThat(isInsideTree(warning, beforeSameStartLine)).isFalse();
      assertThat(isInsideTree(warning, includingSameStartLine)).isTrue();
      assertThat(isInsideTree(warning, including)).isTrue();
      assertThat(isInsideTree(warning, includingSameEndLine)).isTrue();
      assertThat(isInsideTree(warning, afterSameEndLine)).isFalse();
      assertThat(isInsideTree(warning, afterDifferentLine)).isFalse();
    }

    @Test
    void inside_single_line_warning() {
      JWarning warning = new JWarning("message", JWarning.Type.UNUSED_IMPORT, 5, 20, 5, 50);

      Tree beforeCompletely = importTree(3, 0, 3, 10);
      Tree beforeOverlapping = importTree(4, 0, 5, 30);
      Tree beforeSameLine = importTree(5, 0, 5, 15);
      Tree including = importTree(5, 15, 5, 55);
      Tree afterSameLine = importTree(5, 55, 5, 60);
      Tree afterOverlapping = importTree(5, 30, 6, 60);
      Tree afterCompletely = importTree(6, 0, 6, 10);

      assertThat(isInsideTree(warning, beforeCompletely)).isFalse();
      assertThat(isInsideTree(warning, beforeOverlapping)).isFalse();
      assertThat(isInsideTree(warning, beforeSameLine)).isFalse();
      assertThat(isInsideTree(warning, including)).isTrue();
      assertThat(isInsideTree(warning, afterSameLine)).isFalse();
      assertThat(isInsideTree(warning, afterOverlapping)).isFalse();
      assertThat(isInsideTree(warning, afterCompletely)).isFalse();
    }

    @Test
    void inside_wrong_kind() {
      JWarning warning = new JWarning("message", JWarning.Type.UNUSED_IMPORT, 5, 20, 7, 50);

      VariableTree variableTree = variableTree(4, 55, 8, 15);
      ImportTree importTree = importTree(4, 55, 8, 15);

      assertThat(isInsideTree(warning, variableTree)).isFalse();
      assertThat(isInsideTree(warning, importTree)).isTrue();
    }

    @Test
    void test_isMorePreciseTree() {
      // variable is a parent of name and type, so name and type are more precise
      assertThat(isMorePreciseTree(variable, name)).isTrue();
      assertThat(isMorePreciseTree(variable, type)).isTrue();
      // name and type are more precise than their parent variable
      assertThat(isMorePreciseTree(name, variable)).isFalse();
      assertThat(isMorePreciseTree(type, variable)).isFalse();
      // name and type are not overlapping
      assertThat(isMorePreciseTree(name, type)).isFalse();
    }

    @Test
    void test_setSyntaxTree() {
      JWarning warningOnType = new JWarning("message", JWarning.Type.UNUSED_IMPORT, 1, 10, 1, 11);
      assertThat(warningOnType.syntaxTree()).isNull();

      setSyntaxTree(warningOnType, variable);
      assertThat(warningOnType.syntaxTree()).isEqualTo(variable);

      setSyntaxTree(warningOnType, name);
      assertThat(warningOnType.syntaxTree()).isEqualTo(name);

      setSyntaxTree(warningOnType, type);
      // has not been able to change, "type" was not more precise than "name" in terms of tree
      assertThat(warningOnType.syntaxTree()).isEqualTo(name);
    }

    @Test
    void test_matchesTreeExactly() {
      JWarning warningOnType = new JWarning("message", JWarning.Type.UNUSED_IMPORT, 1, 10, 1, 11);

      setSyntaxTree(warningOnType, variable);
      assertThat(matchesTreeExactly(warningOnType)).isFalse();

      setSyntaxTree(warningOnType, type);
      assertThat(matchesTreeExactly(warningOnType)).isTrue();

      JWarning warningOnTypeAgain = new JWarning("message", JWarning.Type.UNUSED_IMPORT, 1, 10, 1, 11);
      setSyntaxTree(warningOnTypeAgain, name);
      assertThat(matchesTreeExactly(warningOnTypeAgain)).isFalse();
    }
  }

  private static ImportTree importTree(int startLine, int startColumn, int endLine, int endColumn) {
    InternalSyntaxToken fakeStartToken = syntaxToken(startLine, startColumn, " ");
    InternalSyntaxToken fakeEndToken = syntaxToken(endLine, endColumn, " ");
    return new JavaTree.ImportTreeImpl(fakeStartToken, null, null, fakeEndToken);
  }

  private static VariableTree variableTree(int startLine, int startColumn, int endLine, int endColumn) {
    IdentifierTreeImpl fakeVariableName = new IdentifierTreeImpl(syntaxToken(endLine, endColumn, " "));
    IdentifierTreeImpl fakeTypeName = new IdentifierTreeImpl(syntaxToken(startLine, startColumn, " "));
    return new VariableTreeImpl(fakeVariableName).completeType(fakeTypeName);
  }

  private static InternalSyntaxToken syntaxToken(int line, int column, String value) {
    return new InternalSyntaxToken(line, column, value, Collections.emptyList(), false);
  }
}
