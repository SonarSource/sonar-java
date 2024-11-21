/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.regex.RegexCheck;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.finders.UnicodeUnawareCharClassFinder;

@Rule(key = "S5867")
public class UnicodeAwareCharClassesCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new UnicodeUnawareCharClassFinder(this::reportIssueFromCommons, (message, cost, secondaries) -> {
      String flagName = methodInvocationOrAnnotation.is(Tree.Kind.ANNOTATION) ? "(?U)" : "UNICODE_CHARACTER_CLASS";
      reportIssue(methodOrAnnotationName(methodInvocationOrAnnotation),
        message.replace("\"u\"", String.format("\"%s\"", flagName)),
        cost,
        secondaries.stream()
          .map(RegexCheck.RegexIssueLocation::fromCommonsRegexIssueLocation)
          .toList());
    }).visit(regexForLiterals);
  }

}
