/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.javac;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTokensTest {

  @Test
  void finds_tokens_and_leading_comments_with_source_positions() {
    String source = "class A {\r\n  /// description\n  String s = \"// not a comment\"; /* block */\n  int n = 1e-2;\n}";
    SourceTokens tokens = new SourceTokens(source);

    SourceTokens.Token string = tokens.firstToken(0, source.length(), "\"// not a comment\"");
    SourceTokens.Token number = tokens.firstToken(0, source.length(), "1e-2");

    assertThat(tokens.tokenAtOrAfter(source.indexOf("String")).text()).isEqualTo("String");
    assertThat(tokens.tokenAtOrBefore(source.indexOf("s =") + 1).text()).isEqualTo("s");
    assertThat(tokens.syntaxToken(tokens.firstToken(0, source.length(), "String")).trivias())
      .extracting(SyntaxTrivia::commentKind)
      .containsExactly(SyntaxTrivia.CommentKind.MARKDOWN);
    assertThat(tokens.syntaxToken(tokens.firstToken(0, source.length(), "int")).trivias())
      .extracting(SyntaxTrivia::commentKind)
      .containsExactly(SyntaxTrivia.CommentKind.BLOCK);
    assertThat(tokens.line(number.start())).isEqualTo(4);
    assertThat(tokens.column(number.start())).isEqualTo(10);
    assertThat(string.start()).isEqualTo(source.indexOf("\"// not a comment\""));
    assertThat(tokens.lastToken(0, source.length(), "}").end()).isEqualTo(source.length());
    assertThat(tokens.tokens().get(tokens.tokens().size() - 1).isEof()).isTrue();
  }

  @Test
  void classifies_triple_slash_comments_by_source_version() {
    String source = "/// documentation\nclass A {}";
    SourceTokens older = new SourceTokens(source, 22);
    SourceTokens current = new SourceTokens(source, 23);

    assertThat(older.syntaxToken(older.firstToken(0, source.length(), "class")).trivias())
      .extracting(SyntaxTrivia::commentKind)
      .containsExactly(SyntaxTrivia.CommentKind.LINE);
    assertThat(current.syntaxToken(current.firstToken(0, source.length(), "class")).trivias())
      .extracting(SyntaxTrivia::commentKind)
      .containsExactly(SyntaxTrivia.CommentKind.MARKDOWN);
  }

  @Test
  void combines_adjacent_markdown_lines_into_one_trivia() {
    String source = "/// Description.\n  /// @param value documented\nvoid method(int value) {}";
    SourceTokens tokens = new SourceTokens(source, 23);

    assertThat(tokens.syntaxToken(tokens.firstToken(0, source.length(), "void")).trivias())
      .extracting(SyntaxTrivia::comment)
      .containsExactly("/// Description.\n  /// @param value documented");
  }

  @Test
  void preserves_comment_before_end_of_file() {
    String source = "class A {}\n// Noncompliant@0";
    SourceTokens tokens = new SourceTokens(source);

    SourceTokens.Token eof = tokens.tokens().get(tokens.tokens().size() - 1);
    assertThat(tokens.syntaxToken(eof).trivias())
      .extracting(SyntaxTrivia::comment)
      .containsExactly("// Noncompliant@0");
  }
}
