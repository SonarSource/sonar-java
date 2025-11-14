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
package org.sonar.java.checks.regex;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonarsource.analyzer.commons.regex.MatchType;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.finders.ReluctantQuantifierWithEmptyContinuationFinder;

@Rule(key = "S6019")
public class ReluctantQuantifierWithEmptyContinuationCheck extends AbstractRegexCheckTrackingMatchType {

  @Override
  protected void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation, MatchType matchType) {
    new ReluctantQuantifierWithEmptyContinuationFinder(this::reportIssueFromCommons, matchType).visit(regexForLiterals);
  }

}
