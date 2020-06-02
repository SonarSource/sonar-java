/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.SyntaxError;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5856")
public class RegexParseErrorCheck extends AbstractRegexCheck {

  private static final String ERROR_MESSAGE = "Fix %s inside this regex.";
  private static final String SINGULAR = "the syntax error";
  private static final String PLURAL = "the syntax errors";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    List<SyntaxError> syntaxErrors = regexForLiterals.getSyntaxErrors();
    if (syntaxErrors.isEmpty()) {
      return;
    }
    String msg;
    if (syntaxErrors.size() > 1) {
      msg = String.format(ERROR_MESSAGE, PLURAL);
    } else {
      msg = String.format(ERROR_MESSAGE, SINGULAR);
    }
    List<RegexIssueLocation> secondaries = syntaxErrors.stream()
      .map(error -> new RegexCheck.RegexIssueLocation(error.getOffendingSyntaxElement(), error.getMessage()))
      .collect(Collectors.toList());

    // report on the first issue
    reportIssue(syntaxErrors.get(0).getOffendingSyntaxElement(), msg, null, secondaries);
  }

}
