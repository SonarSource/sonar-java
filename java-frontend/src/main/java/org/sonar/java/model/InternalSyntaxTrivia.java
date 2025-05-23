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

import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.java.model.location.InternalPosition;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class InternalSyntaxTrivia extends JavaTree implements SyntaxTrivia {


  private final CommentKind commentKind;

  private final String comment;

  @Nonnull
  private final Range range;

  public InternalSyntaxTrivia(CommentKind commentKind, String comment, int line, int columnOffset) {
    this.commentKind = commentKind;
    this.comment = comment;
    boolean mayHaveLineBreaks = commentKind != CommentKind.LINE;
    range = mayHaveLineBreaks
      ? Range.at(InternalPosition.atOffset(line, columnOffset), comment)
      : Range.at(InternalPosition.atOffset(line, columnOffset), comment.length());

    boolean validKind = switch (commentKind) {
      case LINE -> comment.startsWith("//");
      case BLOCK -> comment.startsWith("/*") && comment.endsWith("*/");
      case JAVADOC -> comment.startsWith("/**") && comment.endsWith("*/");
      case MARKDOWN -> comment.startsWith("///");
    };
    if (!validKind) {
      throw new IllegalArgumentException("Invalid comment kind: " + commentKind + " for comment: " + comment);
    }
  }

  @Override
  public String comment() {
    return comment;
  }

  @Override
  public String commentContent() {
    return switch (commentKind) {
      case LINE -> comment.substring(2);
      case BLOCK -> comment.substring(2, comment.length() - 2);
      case JAVADOC -> comment.substring(3, comment.length() - 2);
      case MARKDOWN -> comment.substring(3).replaceAll("\\R[ \t\f]*+///", "\n");
    };
  }

  @Override
  public CommentKind commentKind() {
    return commentKind;
  }

  @Override
  public boolean isComment(CommentKind kind) {
    return commentKind == kind;
  }

  @Override
  public boolean isComment(CommentKind... kinds) {
    for (CommentKind kind : kinds) {
      if (commentKind == kind) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int startLine() {
    return range.start().line();
  }

  @Override
  public Kind kind() {
    return Tree.Kind.TRIVIA;
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  @Override
  public List<Tree> children() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    // do nothing
  }

  @Override
  public int getLine() {
    return range.start().line();
  }

  @Override
  public int column() {
    return range.start().columnOffset();
  }

  @Nonnull
  @Override
  public Range range() {
    return range;
  }

}
