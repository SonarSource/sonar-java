/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.LineUtils.endLine;
import static org.sonar.java.model.LineUtils.splitLines;
import static org.sonar.java.model.LineUtils.startLine;

class LineUtilsTest {

  private static CompilationUnitTree tree;

  @BeforeAll
  static void before() {
    tree = JParserTestUtils.parse("""
      package org.foo;
      
      class A {
       /*
        * trivia
        */
      }""");
  }

  @Test
  void split_lines() {
    assertThat(splitLines(""))
      .containsExactly("");

    assertThat(splitLines("  foo"))
      .containsExactly("  foo");

    assertThat(splitLines("\n"))
      .containsExactly("");

    assertThat(splitLines("foo\n"))
      .containsExactly("foo");

    assertThat(splitLines("\nfoo\n"))
      .containsExactly("", "foo");

    assertThat(splitLines("a\nb"))
      .containsExactly("a", "b");

    assertThat(splitLines("a\nb\n"))
      .containsExactly("a", "b");

    assertThat(splitLines("a\nb\nc"))
      .containsExactly("a", "b", "c");

    assertThat(splitLines("a\nb\nc\n"))
      .containsExactly("a", "b", "c");

    assertThat(splitLines("a\n\nb\r\rc\r\n\r\nd\n\r\n\r"))
      .containsExactly("a", "", "b", "", "c", "", "d", "", "");
  }

  @Test
  void start_Line() {
    assertThat(startLine(tree)).isEqualTo(1);

    ClassTree classTree = (ClassTree) tree.types().get(0);
    SyntaxToken token = classTree.firstToken();
    assertThat(startLine(token)).isEqualTo(3);

    SyntaxTrivia trivia = classTree.lastToken().trivias().get(0);
    assertThat(startLine(trivia)).isEqualTo(4);
  }

  @Test
  void end_Line() {
    assertThat(endLine(tree)).isEqualTo(7);

    ClassTree classTree = (ClassTree) tree.types().get(0);
    SyntaxToken token = classTree.firstToken();
    assertThat(endLine(token)).isEqualTo(3);

    SyntaxTrivia trivia = classTree.lastToken().trivias().get(0);
    assertThat(endLine(trivia)).isEqualTo(6);
  }

}
