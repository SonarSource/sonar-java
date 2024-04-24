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
package org.sonar.java.reporting;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.java.Preconditions;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.quickfixes.TextSpan;

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

  public static TextSpan textSpanFor(Tree syntaxNode) {
    Tree nonEmptyTree = getNonEmptyTree(syntaxNode);
    return textSpanBetween(nonEmptyTree.firstToken(), nonEmptyTree.lastToken());
  }

  public static TextSpan textSpanBetween(Tree startTree, Tree endTree) {
    return textSpanBetween(startTree, true, endTree, true);
  }

  public static TextSpan textSpanBetween(Tree startTree, boolean includeStart, Tree endTree, boolean includeEnd) {
    TextSpan start = AnalyzerMessage.textSpanFor(startTree);
    TextSpan end = AnalyzerMessage.textSpanFor(endTree);
    return new TextSpan(
      includeStart ? start.startLine : start.endLine,
      includeStart ? start.startCharacter : start.endCharacter,
      includeEnd ? end.endLine : end.startLine,
      includeEnd ? end.endCharacter : end.startCharacter
    );
  }

  private static TextSpan textSpanBetween(SyntaxToken firstSyntaxToken, SyntaxToken lastSyntaxToken) {
    Position first = Position.startOf(firstSyntaxToken);
    Position last = Position.endOf(lastSyntaxToken);
    TextSpan location = new TextSpan(
      first.line(),
      first.columnOffset(),
      last.line(),
      last.columnOffset());
    checkLocation(firstSyntaxToken, location);
    return location;
  }

  private static void checkLocation(SyntaxToken firstSyntaxToken, TextSpan location) {
    Position start = Position.startOf(firstSyntaxToken);
    Preconditions.checkState(!location.isEmpty(),
      "Invalid issue location: Text span is empty when trying reporting on (l:%s, c:%s).",
      start.line(), start.column());
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
