/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java;

import com.google.common.base.Preconditions;

import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Replacement for {@link org.sonar.squidbridge.api.CheckMessage}.
 */
public class AnalyzerMessage {

  private final JavaCheck check;
  private final File file;
  private final String message;
  private final int cost;
  @Nullable
  private TextSpan textSpan;
  public final List<List<AnalyzerMessage>> flows = new ArrayList<>();

  public AnalyzerMessage(JavaCheck check, File file, int line, String message, int cost) {
    this(check, file, line > 0 ? new TextSpan(line) : null, message, cost);
  }

  public AnalyzerMessage(JavaCheck check, File file, @Nullable TextSpan textSpan, String message, int cost) {
    this.check = check;
    this.file = file;
    this.message = message;
    this.cost = cost;
    this.textSpan = textSpan;
  }

  public JavaCheck getCheck() {
    return check;
  }

  public File getFile() {
    return file;
  }

  /**
   * @return null, when target of a message - is a file
   */
  @Nullable
  public TextSpan primaryLocation() {
    return textSpan;
  }

  /**
   * @return null, when target of a message - is a file
   */
  @Nullable
  public Integer getLine() {
    return textSpan == null ? null : textSpan.startLine;
  }

  public String getMessage() {
    return message;
  }

  @Nullable
  public Double getCost() {
    return cost > 0 ? (double) cost : null;
  }

  public static final class TextSpan {
    public final int startLine;
    public final int startCharacter;
    public final int endLine;
    public final int endCharacter;

    public TextSpan(int line) {
      this(line, -1, line, -1);
    }

    public TextSpan(int startLine, int startCharacter, int endLine, int endCharacter) {
      this.startLine = startLine;
      this.startCharacter = startCharacter;
      this.endLine = endLine;
      this.endCharacter = endCharacter;
    }

    @Override
    public String toString() {
      return "(" + startLine + ":" + startCharacter + ")-(" + endLine + ":" + endCharacter + ")";
    }

    public boolean onLine() {
      return startCharacter == -1;
    }

    public boolean isEmpty() {
      return startLine == endLine && startCharacter == endCharacter;
    }
  }

  public static AnalyzerMessage.TextSpan textSpanFor(Tree syntaxNode) {
    SyntaxToken firstSyntaxToken = syntaxNode.firstToken();
    SyntaxToken lastSyntaxToken = syntaxNode.lastToken();
    return textSpanBetween(firstSyntaxToken, lastSyntaxToken);
  }

  public static AnalyzerMessage.TextSpan textSpanBetween(Tree startTree, Tree endTree) {
    SyntaxToken firstSyntaxToken = startTree.firstToken();
    SyntaxToken lastSyntaxToken = endTree.lastToken();
    return textSpanBetween(firstSyntaxToken, lastSyntaxToken);
  }

  private static AnalyzerMessage.TextSpan textSpanBetween(SyntaxToken firstSyntaxToken, SyntaxToken lastSyntaxToken) {
    AnalyzerMessage.TextSpan location = new AnalyzerMessage.TextSpan(
      firstSyntaxToken.line(),
      firstSyntaxToken.column(),
      lastSyntaxToken.line(),
      lastSyntaxToken.column() + lastSyntaxToken.text().length()
    );
    Preconditions.checkState(!location.isEmpty(),
      "Invalid issue location: Text span is empty when trying reporting on (l:%s, c:%s).",
      firstSyntaxToken.line(), firstSyntaxToken.column());
    return location;
  }

  @Override
  public String toString() {
    return String.format("'%s' in %s:%d", getMessage(), getFile(), getLine());
  }
}
