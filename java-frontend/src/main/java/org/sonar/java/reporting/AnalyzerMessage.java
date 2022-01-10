/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.reporting;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.java.Preconditions;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Class used to represent analyzer issue messages
 */
public class AnalyzerMessage {

  private final JavaCheck check;
  private final InputComponent inputComponent;
  private final String message;
  private final int cost;
  @Nullable
  private TextSpan textSpan;
  public final List<List<AnalyzerMessage>> flows = new ArrayList<>();

  public AnalyzerMessage(JavaCheck check, InputComponent inputComponent, int line, String message, int cost) {
    this(check, inputComponent, line > 0 ? new TextSpan(line) : null, message, cost);
  }

  public AnalyzerMessage(JavaCheck check, InputComponent inputComponent, @Nullable TextSpan textSpan, String message, int cost) {
    this.check = check;
    this.inputComponent = inputComponent;
    this.message = message;
    this.cost = cost;
    this.textSpan = textSpan;
  }

  public JavaCheck getCheck() {
    return check;
  }

  public InputComponent getInputComponent() {
    return inputComponent;
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

    @Override
    public int hashCode() {
      int prime = 27;
      int result = 1;
      result = prime * result + startLine;
      result = prime * result + startCharacter;
      result = prime * result + endLine;
      result = prime * result + endCharacter;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TextSpan)) {
        return false;
      }
      TextSpan other = (TextSpan) obj;
      return startLine == other.startLine
        && startCharacter == other.startCharacter
        && endLine == other.endLine
        && endCharacter == other.endCharacter;
    }

  }

  public static AnalyzerMessage.TextSpan textSpanFor(Tree syntaxNode) {
    Tree nonEmptyTree = getNonEmptyTree(syntaxNode);
    return textSpanBetween(nonEmptyTree.firstToken(), nonEmptyTree.lastToken());
  }

  public static AnalyzerMessage.TextSpan textSpanBetween(Tree startTree, Tree endTree) {
    return textSpanBetween(startTree, true, endTree, true);
  }

  public static AnalyzerMessage.TextSpan textSpanBetween(Tree startTree, boolean includeStart, Tree endTree, boolean includeEnd) {
    AnalyzerMessage.TextSpan start = AnalyzerMessage.textSpanFor(startTree);
    AnalyzerMessage.TextSpan end = AnalyzerMessage.textSpanFor(endTree);
    return new AnalyzerMessage.TextSpan(
      includeStart ? start.startLine : start.endLine,
      includeStart ? start.startCharacter : start.endCharacter,
      includeEnd ? end.endLine : end.startLine,
      includeEnd ? end.endCharacter : end.startCharacter
    );
  }

  private static AnalyzerMessage.TextSpan textSpanBetween(SyntaxToken firstSyntaxToken, SyntaxToken lastSyntaxToken) {
    Range first = firstSyntaxToken.range();
    Range last = lastSyntaxToken.range();
    AnalyzerMessage.TextSpan location = new AnalyzerMessage.TextSpan(
      first.start().line(),
      first.start().columnOffset(),
      last.end().line(),
      last.end().columnOffset());
    checkLocation(firstSyntaxToken, location);
    return location;
  }

  private static void checkLocation(SyntaxToken firstSyntaxToken, TextSpan location) {
    Preconditions.checkState(!location.isEmpty(),
      "Invalid issue location: Text span is empty when trying reporting on (l:%s, c:%s).",
      firstSyntaxToken.range().start().line(), firstSyntaxToken.range().start().column());
  }

  /**
   * It's possible that tree has no source code to match, thus no tokens. For example {@code InferedTypeTree}.
   * In this case we will report on a parent tree.
   * @return tree itself if it's not empty or its first non-empty ancestor
   */
  private static Tree getNonEmptyTree(Tree tree) {
    if (tree.firstToken() != null) {
      return tree;
    }

    Tree parent = tree.parent();
    if (parent != null) {
      return getNonEmptyTree(parent);
    }

    throw new IllegalStateException("Trying to report on an empty tree with no parent");
  }

  @Override
  public String toString() {
    return String.format("'%s' in %s:%d", message, inputComponent, getLine());
  }
}
