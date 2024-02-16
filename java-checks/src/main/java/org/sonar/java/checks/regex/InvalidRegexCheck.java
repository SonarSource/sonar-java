/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.regex;

import java.util.List;
import java.util.stream.Collectors;
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
