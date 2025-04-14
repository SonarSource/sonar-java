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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.SyntaxTrivia.CommentKind;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalSyntaxTriviaTest {

  @Test
  void line_comment() {
    SyntaxTrivia trivia = new InternalSyntaxTrivia(CommentKind.LINE, "// comment", 42, 21);
    assertThat(trivia.comment()).isEqualTo("// comment");
    assertThat(trivia.commentContent()).isEqualTo(" comment");
    assertThat(trivia.commentKind()).isEqualTo(CommentKind.LINE);
    assertThat(trivia.isComment(CommentKind.LINE)).isTrue();
    assertThat(trivia.isComment(CommentKind.BLOCK)).isFalse();
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
  void block_comment() {
    SyntaxTrivia trivia = new InternalSyntaxTrivia(CommentKind.BLOCK, "/* line1\n   line2 */", 42, 21);
    assertThat(trivia.comment()).isEqualTo("/* line1\n   line2 */");
    assertThat(trivia.commentContent()).isEqualTo(" line1\n   line2 ");
    assertThat(trivia.commentKind()).isEqualTo(CommentKind.BLOCK);
    assertThat(trivia.startLine()).isEqualTo(42);
    assertThat(trivia.column()).isEqualTo(21);
    assertThat(trivia.range()).isEqualTo(Range.at(42, 22, 43, 12));

    JavaTree tree = (JavaTree) trivia;
    assertThat(tree.getLine()).isEqualTo(42);
  }

  @Test
  void javadoc_comment() {
    SyntaxTrivia trivia = new InternalSyntaxTrivia(CommentKind.JAVADOC, "/** method\n  * foo */", 42, 21);
    assertThat(trivia.comment()).isEqualTo("/** method\n  * foo */");
    assertThat(trivia.commentContent()).isEqualTo(" method\n  * foo ");
    assertThat(trivia.commentKind()).isEqualTo(CommentKind.JAVADOC);
    assertThat(trivia.startLine()).isEqualTo(42);
    assertThat(trivia.column()).isEqualTo(21);
    assertThat(trivia.range()).isEqualTo(Range.at(42, 22, 43, 11));

    JavaTree tree = (JavaTree) trivia;
    assertThat(tree.getLine()).isEqualTo(42);
  }

  @Test
  void markdown_comment() {
    SyntaxTrivia trivia = new InternalSyntaxTrivia(CommentKind.MARKDOWN, "/// line1\r\n  /// line2\r  /// line3\n  ///", 42, 21);
    assertThat(trivia.comment()).isEqualTo("/// line1\r\n  /// line2\r  /// line3\n  ///");
    assertThat(trivia.commentContent()).isEqualTo(" line1\n line2\n line3\n");
    assertThat(trivia.commentKind()).isEqualTo(CommentKind.MARKDOWN);
    assertThat(trivia.startLine()).isEqualTo(42);
    assertThat(trivia.column()).isEqualTo(21);
    assertThat(trivia.range()).isEqualTo(Range.at(42, 22, 45, 6));

    JavaTree tree = (JavaTree) trivia;
    assertThat(tree.getLine()).isEqualTo(42);
  }

  @Test
  void invalid_comment_kind() {
    assertThatThrownBy(() -> new InternalSyntaxTrivia(CommentKind.LINE, "/* invalid */", 42, 21))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid comment kind: LINE for comment: /* invalid */");
    assertThatThrownBy(() -> new InternalSyntaxTrivia(CommentKind.BLOCK, "// invalid", 42, 21))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid comment kind: BLOCK for comment: // invalid");
    assertThatThrownBy(() -> new InternalSyntaxTrivia(CommentKind.BLOCK, "/* broken", 42, 21))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid comment kind: BLOCK for comment: /* broken");
    assertThatThrownBy(() -> new InternalSyntaxTrivia(CommentKind.JAVADOC, "/* invalid */", 42, 21))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid comment kind: JAVADOC for comment: /* invalid */");
    assertThatThrownBy(() -> new InternalSyntaxTrivia(CommentKind.JAVADOC, "/** broken", 42, 21))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid comment kind: JAVADOC for comment: /** broken");
  }

}
