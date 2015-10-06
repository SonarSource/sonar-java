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
package org.sonar.java.se;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.fest.assertions.Fail;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceCode;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;

/**
 * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
 * But usually better to use line number relative to the current, this is possible to do by prefixing the number with either '+' or '-'.
 * For example:
 * <pre>
 *   // Noncompliant@+1 {{do not import "java.util.List"}}
 *   import java.util.List;
 * </pre>
 */
public class JavaCheckVerifier extends SubscriptionVisitor {

  private static final String TRIGGER = "// Noncompliant";
  private ArrayListMultimap<Integer, String> expected = ArrayListMultimap.create();

  public static void verify(String filename, JavaFileScanner check) {
    scanFile(filename, check, new JavaCheckVerifier());
  }

  private static void scanFile(String filename, JavaFileScanner check, JavaCheckVerifier javaCheckVerifier) {
    JavaAstScanner.scanSingleFile(new File(filename),
      new VisitorsBridge(Lists.newArrayList(check, javaCheckVerifier), Lists.newArrayList(Lists.newArrayList(new File("target/test-classes"))), null));
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
  }

  private void collectExpectedIssues(String comment, int line) {
    if (comment.startsWith(TRIGGER)) {
      comment = StringUtils.remove(comment, TRIGGER);

      String expectedMessage = StringUtils.substringBetween(comment, "{{", "}}");

      comment = StringUtils.stripEnd(StringUtils.remove(comment, "{{" + expectedMessage + "}}"), " \t");
      if (StringUtils.startsWith(comment, "@")) {
        final int lineAdjustment;
        final char firstChar = comment.charAt(1);
        final int endIndex = comment.indexOf(' ');
        if (endIndex == -1) {
          lineAdjustment = Integer.parseInt(comment.substring(2));
          comment = "";
        } else {
          lineAdjustment = Integer.parseInt(comment.substring(2, endIndex));
          comment = comment.substring(endIndex + 1).trim();
        }
        if (firstChar == '+') {
          line += lineAdjustment;
        } else if (firstChar == '-') {
          line -= lineAdjustment;
        }
      }
      comment = StringUtils.trim(comment);
      int times = StringUtils.isEmpty(comment) ? 1 : Integer.parseInt(comment);
      for (int i = 0; i < times; i++) {
        expected.put(line, expectedMessage);
      }
    }
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    expected.clear();
    super.scanFile(context);
    VisitorsBridge.DefaultJavaFileScannerContext djfsc = (VisitorsBridge.DefaultJavaFileScannerContext) context;
    // leave file.
    checkIssues(djfsc.sourceFile);
    expected.clear();
  }

  private void checkIssues(SourceCode sourceCode) {
    Preconditions.checkState(sourceCode.hasCheckMessages(), "At least one issue expected");
    List<Integer> unexpectedLines = Lists.newLinkedList();
    for (CheckMessage checkMessage : sourceCode.getCheckMessages()) {
      int line = checkMessage.getLine();
      if (!expected.containsKey(line)) {
        unexpectedLines.add(line);
      } else {
        List<String> list = expected.get(line);
        String expectedMessage = list.remove(list.size() - 1);
        if (expectedMessage != null) {
          assertThat(checkMessage.getText(Locale.US)).isEqualTo(expectedMessage);
        }
      }
    }
    if (!expected.isEmpty() || !unexpectedLines.isEmpty()) {
      Collections.sort(unexpectedLines);
      String expectedMsg = !expected.isEmpty() ? "Expected " + expected : "";
      String unexpectedMsg = !unexpectedLines.isEmpty() ? (expectedMsg.isEmpty() ? "" : ", ") + "Unexpected at " + unexpectedLines : "";
      throw Fail.fail(expectedMsg + unexpectedMsg);
    }

  }

}
