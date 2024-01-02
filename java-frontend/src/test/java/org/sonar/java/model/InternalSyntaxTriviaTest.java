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
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalSyntaxTriviaTest {

  @Test
  void single_line_comment() {
    SyntaxTrivia trivia = InternalSyntaxTrivia.create("// comment", 42, 21);
    assertThat(trivia.comment()).isEqualTo("// comment");
    assertThat(trivia.startLine()).isEqualTo(42);
    assertThat(trivia.column()).isEqualTo(21);
    assertThat(trivia.range()).isEqualTo(Range.at(42, 22, 42, 32));

    JavaTree tree = (JavaTree) trivia;
    assertThat(tree.getLine()).isEqualTo(42);

    assertThat(tree.kind()).isEqualTo(Tree.Kind.TRIVIA);
    assertThat(tree.isLeaf()).isTrue();
    assertThatThrownBy(tree::children).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void multi_line_comment() {
    SyntaxTrivia trivia = InternalSyntaxTrivia.create("/* line1\n   line2 */", 42, 21);
    assertThat(trivia.comment()).isEqualTo("/* line1\n   line2 */");
    assertThat(trivia.startLine()).isEqualTo(42);
    assertThat(trivia.column()).isEqualTo(21);
    assertThat(trivia.range()).isEqualTo(Range.at(42, 22, 43, 12));

    JavaTree tree = (JavaTree) trivia;
    assertThat(tree.getLine()).isEqualTo(42);
  }

}
