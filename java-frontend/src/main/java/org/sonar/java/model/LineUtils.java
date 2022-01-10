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

public final class LineUtils {

  private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\r\n|[\n\r]");

  private LineUtils() {
    // utility class
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
