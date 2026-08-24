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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S9359")
public class OctalEscapeSequenceFollowedByDigitCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Kind.STRING_LITERAL, Kind.TEXT_BLOCK);
  }

  @Override
  public void visitNode(Tree node) {
    if (LiteralUtils.isEmptyString(node)) {
      return;
    }
    String value = LiteralUtils.trimQuotes(((LiteralTree) node).value());
    if (node.is(Kind.TEXT_BLOCK)) {
      value = value.replaceAll("(\\r?\\n|\\r)\\s*", "");
    }

    int i = 0;
    while (i < value.length()) {
      char c = value.charAt(i);
      if (c == '\\') {
        // Skip escaped backslash
        if (i + 1 < value.length() && value.charAt(i + 1) == '\\') {
          i += 2;
          continue;
        }
        // Check for octal escape followed by digit or another escape
        if (i + 1 < value.length()) {
          char next = value.charAt(i + 1);
          if (isOctalDigit(next)) {
            int escapeEnd = findEscapeEnd(value, i);
            if (escapeEnd < value.length() && isAmbiguousFollowUp(value.charAt(escapeEnd))) {
              reportIssue(node, "Remove this octal escape sequence or separate it from the following digit.");
              return;
            }
            i = escapeEnd;
            continue;
          }
        }
      }
      i++;
    }
  }

  private static boolean isOctalDigit(char c) {
    return c >= '0' && c <= '7';
  }

  private static int findEscapeEnd(String value, int start) {
    int escapeEnd = start + 2;
    while (escapeEnd < value.length()
        && isOctalDigit(value.charAt(escapeEnd))
        && escapeEnd - start < 4) {
      escapeEnd++;
    }
    return escapeEnd;
  }

  private static boolean isAmbiguousFollowUp(char c) {
    return c >= '0' && c <= '9' || c == '\\';
  }
}
