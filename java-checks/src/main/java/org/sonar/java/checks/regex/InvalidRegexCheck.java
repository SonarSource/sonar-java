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
package org.sonar.java.checks.regex;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexCheck;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.SyntaxError;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S5856")
public class InvalidRegexCheck extends AbstractRegexCheck {

  private static final String ERROR_MESSAGE = "Fix the syntax error%s inside this regex.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    List<SyntaxError> syntaxErrors = regexForLiterals.getSyntaxErrors();
    if (!syntaxErrors.isEmpty()) {
      reportSyntaxErrors(syntaxErrors);
    }
  }

  private void reportSyntaxErrors(List<SyntaxError> syntaxErrors) {
    // report on the first issue
    RegexSyntaxElement tree = syntaxErrors.get(0).getOffendingSyntaxElement();
    List<RegexIssueLocation> secondaries = syntaxErrors.stream()
      .map(error -> new RegexCheck.RegexIssueLocation(error.getOffendingSyntaxElement(), error.getMessage()))
      .toList();

    reportIssue(tree, secondaries);
  }

  private void reportIssue(RegexSyntaxElement tree, List<RegexIssueLocation> secondaries) {
    String msg = String.format(ERROR_MESSAGE, secondaries.size() > 1 ? "s" : "");
    reportIssue(tree, msg, null, secondaries);
  }

}
