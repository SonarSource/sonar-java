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
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

public final class LineUtils {

  private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\r\n|[\n\r]");

  private LineUtils() {
    // utility class
  }

  public static int startLine(Tree tree) {
    return startLine(tree.firstToken());
  }

  public static int endLine(Tree tree) {
    return endLine(tree.lastToken());
  }

  public static int startLine(SyntaxToken token) {
    return Position.startOf(token).line();
  }

  public static int endLine(SyntaxToken token) {
    return Position.endOf(token).line();
  }

  public static int startLine(SyntaxTrivia trivia) {
    return Position.startOf(trivia).line();
  }

  public static int endLine(SyntaxTrivia trivia) {
    return Position.endOf(trivia).line();
  }

  /**
   * @return for example:
   * "" => { "" }
   * "a" => { "a" }
   * "a\n" => { "a" }
   * "a\nb" => { "a", "b" }
   * "a\nb\n" => { "a", "b" }
   */
  public static List<String> splitLines(String content) {
    List<String> lines = new ArrayList<>();
    Matcher matcher = LINE_BREAK_PATTERN.matcher(content);
    int pos = 0;
    while (matcher.find()) {
      lines.add(content.substring(pos, matcher.start()));
      pos = matcher.end();
    }
    if (pos == 0 || pos < content.length()) {
      lines.add(content.substring(pos));
    }
    return lines;
  }

}
