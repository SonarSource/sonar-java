/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.location.Position;
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

  private static final String RECOMMENDATION_YEAR_MESSAGE = "Use \"%s\" instead of \"%s\" to output the year.";

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
      String message = String.format(RECOMMENDATION_YEAR_MESSAGE, replacement, firstYSeq);
      InternalJavaIssueBuilder issueBuilder = QuickFixHelper.newIssue(context)
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

  private static JavaQuickFix computeQuickFix(ExpressionTree argument,
    int literalContentStartIndex, int literalContentEndIndex, String replacement) {

    Position stringLiteralStart = Position.startOf(argument);
    int quoteDelimiterLength = 1;
    return JavaQuickFix.newQuickFix("Replace year format")
      .addTextEdit(JavaTextEdit.replaceTextSpan(new AnalyzerMessage.TextSpan(
        stringLiteralStart.line(),
        stringLiteralStart.columnOffset() + quoteDelimiterLength + literalContentStartIndex,
        stringLiteralStart.line(),
        stringLiteralStart.columnOffset() + quoteDelimiterLength + literalContentEndIndex),
        replacement))
      .build();
  }

}
