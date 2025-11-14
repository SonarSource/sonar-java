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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2275")
public class PrintfFailCheck extends AbstractPrintfChecker {

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    boolean isMessageFormat = MESSAGE_FORMAT.matches(mit);
    if (isMessageFormat && !mit.methodSymbol().isStatic()) {
      // only consider the static method
      return;
    }
    super.checkFormatting(mit, isMessageFormat);
  }

  @Override
  protected void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    List<String> params = getParameters(formatString, mit);
    cleanupLineSeparator(params);
    if (!params.isEmpty()) {
      if (checkArgumentNumber(mit, argIndexes(params).size(), args.size())) {
        return;
      }
      verifyParametersForErrors(mit, args, params);
    }
  }

  @Override
  protected void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    String newFormatString = cleanupDoubleQuote(formatString);
    checkUnbalancedBraces(mit, newFormatString);
  }

  private void checkUnbalancedBraces(MethodInvocationTree mit, String formatString) {
    String withoutParam = MESSAGE_FORMAT_PATTERN.matcher(formatString).replaceAll("");
    int numberOpenBrace = 0;
    for (int i = 0; i < withoutParam.length(); ++i) {
      char ch = withoutParam.charAt(i);
      switch (ch) {
        case '{':
          numberOpenBrace++;
          break;
        case '}':
          numberOpenBrace--;
          break;
        default:
          break;
      }
    }
    if (numberOpenBrace > 0) {
      reportIssue(mit.arguments().get(0), "Single left curly braces \"{\" must be escaped.");
    }
  }

  @Override
  protected void handlePrintfFormatCatchingErrors(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    // Do nothing, since the method invocation will catch the error.
  }

  @Override
  protected void handleOtherFormatTree(MethodInvocationTree mit, ExpressionTree formatTree, List<ExpressionTree> args) {
    // do nothing
  }

  @Override
  protected void reportMissingPrevious(MethodInvocationTree mit) {
    reportIssue(mit, "The argument index '<' refers to the previous format specifier but there isn't one.");
  }

}
