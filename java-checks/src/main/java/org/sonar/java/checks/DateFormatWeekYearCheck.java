/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3986")
public class DateFormatWeekYearCheck extends AbstractMethodDetection {
  private static final MethodMatchers SIMPLE_DATE_FORMAT_MATCHER = MethodMatchers.create()
    .ofTypes("java.text.SimpleDateFormat")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers OF_PATTERN_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatter")
    .names("ofPattern")
    .addParametersMatcher("java.lang.String")
    .addParametersMatcher("java.lang.String", "java.util.Locale")
    .build();

  private static final String RECOMMENDATION_YEAR_MESSAGE = "Make sure that week Year \"%s\" is expected here instead of Year \"%s\".";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(SIMPLE_DATE_FORMAT_MATCHER, OF_PATTERN_MATCHER);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (newClassTree.arguments().isEmpty()) {
      return;
    }
    ExpressionTree expressionTree = newClassTree.arguments().get(0);
    inspectPattern(expressionTree);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    if (arguments.isEmpty()) {
      return;
    }
    ExpressionTree argument = arguments.get(0);
    inspectPattern(argument);
  }

  private void inspectPattern(ExpressionTree argument) {
    Optional<String> literal = argument.asConstant(String.class);
    if (!literal.isPresent()) {
      return;
    }
    String datePattern = literal.get();
    if (StringUtils.contains(datePattern, 'w')) {
      return;
    }
    int start = datePattern.indexOf('Y');
    if (start > -1) {
      int end = getEndIndexOfYearSequence(datePattern, start);
      String firstYSeq = datePattern.substring(start, end);
      String replacement = firstYSeq.toLowerCase(Locale.ENGLISH);
      String message = String.format(RECOMMENDATION_YEAR_MESSAGE, firstYSeq, replacement);
      InternalJavaIssueBuilder issueBuilder = ((InternalJavaIssueBuilder) ((DefaultJavaFileScannerContext) context).newIssue())
        .forRule(this)
        .onTree(argument)
        .withMessage(message);
      if (argument.is(Tree.Kind.STRING_LITERAL)) {
        issueBuilder.withQuickFix(() -> computeQuickFix(argument, start, end, replacement));
      }
      issueBuilder.report();
    }
  }

  private static int getEndIndexOfYearSequence(String sequence, int start) {
    int count = start;
    while (count < sequence.length() && sequence.charAt(count) == 'Y') {
      count++;
    }
    return count;
  }

  private static JavaQuickFix computeQuickFix(ExpressionTree argument, int startColumn, int endColumn, String replacement) {
    SyntaxToken firstToken = argument.firstToken();
    AnalyzerMessage.TextSpan textSpan = computeTextSpan(firstToken, startColumn, endColumn);
    return JavaQuickFix.newQuickFix("Replace year format")
      .addTextEdit(JavaTextEdit.replaceTextSpan(textSpan, replacement))
      .build();
  }

  private static AnalyzerMessage.TextSpan computeTextSpan(SyntaxToken firstToken, int startCharacter, int endCharacter) {
    int line = firstToken.line();
    // Columns are 0-based in the AST and need to be adjusted by 1 to suggest a proper quick fix
    int column = firstToken.column() + 1;
    return new AnalyzerMessage.TextSpan(line, column + startCharacter, line, column + endCharacter);
  }
}
