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

import java.util.List;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.MatchType;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;

public abstract class AbstractRegexCheckTrackingMatchType extends AbstractRegexCheckTrackingMatchers {

  private static final MethodMatchers PARTIAL_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.util.regex.Pattern", "java.lang.String")
      .names("split", "splitAsStream", "asPredicate", "replaceAll", "replaceFirst")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.util.regex.Matcher")
      .names("find")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(LANG3_REGEX_UTILS)
      .names("removeAll", "removeFirst", "removePattern", "replaceAll", "replaceFirst", "replacePattern")
      .withAnyParameters()
      .build()
  );

  private static final MethodMatchers FULL_MATCHERS = MethodMatchers.create()
      .ofTypes("java.util.regex.Pattern", "java.util.regex.Matcher", "java.lang.String")
      .names("matches", "asMatchPredicate")
      .withAnyParameters()
      .build();

  private static final MethodMatchers MATCHERS = MethodMatchers.or(PARTIAL_MATCHERS, FULL_MATCHERS);

  protected abstract void checkRegex(RegexParseResult regex, ExpressionTree methodInvocationOrAnnotation, MatchType matchType);

  @Override
  protected MethodMatchers trackedMethodMatchers() {
    return MATCHERS;
  }

  @Override
  protected void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation, List<MethodInvocationTree> trackedMethodsCalled, boolean didEscape) {
    MatchType matchType;
    if (methodInvocationOrAnnotation.is(Tree.Kind.ANNOTATION)) {
      matchType = MatchType.FULL;
    } else if (didEscape) {
      matchType = MatchType.UNKNOWN;
    } else {
      boolean partial = trackedMethodsCalled.stream().anyMatch(PARTIAL_MATCHERS::matches);
      boolean full = trackedMethodsCalled.stream().anyMatch(FULL_MATCHERS::matches);
      if (partial && full) {
        matchType = MatchType.BOTH;
      } else if (partial) {
        matchType = MatchType.PARTIAL;
      } else if (full) {
        matchType = MatchType.FULL;
      } else {
        matchType = MatchType.UNKNOWN;
      }
    }
    checkRegex(regexForLiterals, methodInvocationOrAnnotation, matchType);
  }
}
