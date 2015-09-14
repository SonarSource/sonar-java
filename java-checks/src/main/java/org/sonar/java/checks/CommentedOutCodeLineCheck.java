/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.recognizer.CodeRecognizer;

import java.util.Collections;
import java.util.List;

@Rule(
  key = "CommentedOutCodeLine",
  name = "Sections of code should not be \"commented out\"",
  tags = {"misra", "unused"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class CommentedOutCodeLineCheck extends SubscriptionBaseVisitor {

  private static final double THRESHOLD = 0.9;

  private final CodeRecognizer codeRecognizer;

  private List<SyntaxTrivia> comments;

  public CommentedOutCodeLineCheck() {
    codeRecognizer = new CodeRecognizer(THRESHOLD, new JavaFootprint());
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    comments = Lists.newArrayList();
    super.scanFile(context);
    leaveFile();
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    if (!isHeader(syntaxTrivia) && !isJavadoc(syntaxTrivia.comment()) && !isJSNI(syntaxTrivia.comment())) {
      comments.add(syntaxTrivia);
    }
  }

  /**
   * We assume that comment on a first line - is a header with license.
   * However possible to imagine corner case: file may contain commented-out code starting from first line.
   * But we assume that probability of this is really low.
   */
  private static boolean isHeader(SyntaxTrivia syntaxTrivia) {
    return syntaxTrivia.startLine() == 1;
  }

  /**
   * Detects commented-out code in remaining candidates.
   */
  private void leaveFile() {
    List<Integer> commentedOutCodeLines = Lists.newArrayList();
    for (SyntaxTrivia syntaxTrivia : comments) {
      String[] lines = syntaxTrivia.comment().split("\r\n?|\n");
      for (int i = 0; i < lines.length; i++) {
        if (codeRecognizer.isLineOfCode(lines[i])) {
          // Mark all remaining lines from this comment as a commented out lines of code
          for (int j = i; j < lines.length; j++) {
            commentedOutCodeLines.add(syntaxTrivia.startLine() + j);
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
        addIssue(current, "This block of commented-out lines of code should be removed.");
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
  private static boolean isJavadoc(String comment) {
    return StringUtils.startsWith(comment, "/**");
  }

  /**
   * TODO more precise
   * From GWT documentation:
   * JSNI methods are declared native and contain JavaScript code in a specially formatted comment block
   * between the end of the parameter list and the trailing semicolon.
   * A JSNI comment block begins with the exact token {@link #START_JSNI} and ends with the exact token {@link #END_JSNI}.
   */
  private static boolean isJSNI(String comment) {
    return StringUtils.startsWith(comment, START_JSNI) && StringUtils.endsWith(comment, END_JSNI);
  }

  private static final String START_JSNI = "/*-{";
  private static final String END_JSNI = "}-*/";

}
