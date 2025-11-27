/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks;

import org.apache.commons.lang3.Strings;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

public class CommentContainsPatternChecker {

  private final IssuableSubscriptionVisitor newCheck;
  private final String pattern;
  private final String message;

  public CommentContainsPatternChecker(IssuableSubscriptionVisitor check, String pattern, String message) {
    this.newCheck = check;
    this.pattern = pattern;
    this.message = message;
  }

  private static boolean isLetterAround(String line, String pattern) {
    int start = Strings.CI.indexOf(line, pattern);
    int end = start + pattern.length();

    boolean pre = start > 0 && Character.isLetter(line.charAt(start - 1));
    boolean post = end < line.length() - 1 && Character.isLetter(line.charAt(end));

    return pre || post;
  }

  public void checkTrivia(SyntaxTrivia syntaxTrivia) {
    String comment = syntaxTrivia.comment();
    if (Strings.CI.contains(comment, pattern)) {
      String[] lines = comment.split("\r\n?|\n");
      for (int i = 0; i < lines.length; i++) {
        if (Strings.CI.contains(lines[i], pattern) && !isLetterAround(lines[i], pattern)) {
          newCheck.addIssue(LineUtils.startLine(syntaxTrivia) + i, message);
        }
      }
    }
  }
}
