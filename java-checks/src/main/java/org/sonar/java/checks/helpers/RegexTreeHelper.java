/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.ast.CharacterTree;
import org.sonar.java.regex.ast.RegexSyntaxElement;

public class RegexTreeHelper {

  // M (Mark) is "a character intended to be combined with another character (e.g. accents, umlauts, enclosing boxes, etc.)."
  // See https://www.regular-expressions.info/unicode.html
  private static final Pattern MARK_PATTERN = Pattern.compile("\\p{M}");

  private RegexTreeHelper() {
    // Utils class
  }

  public static List<RegexCheck.RegexIssueLocation> getGraphemeInList(List<? extends RegexSyntaxElement> trees) {
    List<RegexCheck.RegexIssueLocation> result = new ArrayList<>();
    RegexSyntaxElement startGrapheme = null;
    RegexSyntaxElement endGrapheme = null;
    for (RegexSyntaxElement child : trees) {
      if (child instanceof CharacterTree) {
        CharacterTree currentCharacter = (CharacterTree) child;
        if (!currentCharacter.isEscapeSequence()) {
          if (!isMark(currentCharacter)) {
            addCurrentGrapheme(result, startGrapheme, endGrapheme);
            startGrapheme = child;
            endGrapheme = null;
          } else if (startGrapheme != null) {
            endGrapheme = child;
          }
          continue;
        }
      }
      addCurrentGrapheme(result, startGrapheme, endGrapheme);
      startGrapheme = null;
      endGrapheme = null;
    }
    addCurrentGrapheme(result, startGrapheme, endGrapheme);
    return result;
  }

  private static boolean isMark(CharacterTree currentChar) {
    return MARK_PATTERN.matcher(currentChar.characterAsString()).matches();
  }

  private static void addCurrentGrapheme(List<RegexCheck.RegexIssueLocation> result, @Nullable RegexSyntaxElement start, @Nullable RegexSyntaxElement end) {
    if (start != null && end != null) {
      result.add(new RegexCheck.RegexIssueLocation(start, end, ""));
    }
  }

}
