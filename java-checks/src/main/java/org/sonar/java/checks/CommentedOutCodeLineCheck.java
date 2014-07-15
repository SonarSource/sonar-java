/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.squidbridge.checks.SquidCheck;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.recognizer.CodeRecognizer;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Collections;
import java.util.List;

@Rule(key = "CommentedOutCodeLine", priority = Priority.MAJOR,
  tags={"unused"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CommentedOutCodeLineCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private static final double THRESHOLD = 0.9;

  private final CodeRecognizer codeRecognizer;

  private List<Token> comments;

  public CommentedOutCodeLineCheck() {
    codeRecognizer = new CodeRecognizer(THRESHOLD, new JavaFootprint());
  }

  @Override
  public void visitFile(AstNode astNode) {
    comments = Lists.newArrayList();
  }

  /**
   * Creates candidates for commented-out code - all comment blocks.
   */
  @Override
  public void visitToken(Token token) {
    for (Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        Token comment = trivia.getToken();
        if (!isHeader(comment) && !isJavadoc(comment.getOriginalValue()) && !isJSNI(comment.getOriginalValue())) {
          comments.add(trivia.getToken());
        }
      }
    }
  }

  /**
   * We assume that comment on a first line - is a header with license.
   * However possible to imagine corner case: file may contain commented-out code starting from first line.
   * But we assume that probability of this is really low.
   */
  private static boolean isHeader(Token comment) {
    return comment.getLine() == 1;
  }

  /**
   * Detects commented-out code in remaining candidates.
   */
  @Override
  public void leaveFile(AstNode astNode) {
    List<Integer> commentedOutCodeLines = Lists.newArrayList();
    for (Token comment : comments) {
      String[] lines = getContext().getCommentAnalyser().getContents(comment.getOriginalValue()).split("(\r)?\n|\r", -1);
      for (int i = 0; i < lines.length; i++) {
        if (codeRecognizer.isLineOfCode(lines[i])) {
          // Mark all remaining lines from this comment as a commented out lines of code
          for (int j = i; j < lines.length; j++) {
            commentedOutCodeLines.add(comment.getLine() + j);
          }
          break;
        }
      }
    }

    // Greedy algorithm to split lines on blocks and to report only one violation per block
    Collections.sort(commentedOutCodeLines);
    int prev = Integer.MIN_VALUE;
    for (int i = 0; i < commentedOutCodeLines.size(); i++) {
      int current = commentedOutCodeLines.get(i);
      if (prev + 1 < current) {
        getContext().createLineViolation(this, "This block of commented-out lines of code should be removed.", current);
      }
      prev = current;
    }

    comments = null;
  }

  /**
   * TODO more precise
   * From documentation for Javadoc-tool:
   * Documentation comments should be recognized only when placed
   * immediately before class, interface, constructor, method, or field declarations.
   */
  private boolean isJavadoc(String comment) {
    return StringUtils.startsWith(comment, "/**");
  }

  /**
   * TODO more precise
   * From GWT documentation:
   * JSNI methods are declared native and contain JavaScript code in a specially formatted comment block
   * between the end of the parameter list and the trailing semicolon.
   * A JSNI comment block begins with the exact token {@link #START_JSNI} and ends with the exact token {@link #END_JSNI}.
   */
  private boolean isJSNI(String comment) {
    return StringUtils.startsWith(comment, START_JSNI) && StringUtils.endsWith(comment, END_JSNI);
  }

  private static final String START_JSNI = "/*-{";
  private static final String END_JSNI = "}-*/";

}
