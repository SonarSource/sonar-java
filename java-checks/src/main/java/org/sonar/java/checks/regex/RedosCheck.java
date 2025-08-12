/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonarsource.analyzer.commons.regex.MatchType;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.finders.RedosFinder;

@Rule(key = "S5852")
public class RedosCheck extends AbstractRegexCheckTrackingMatchType {

  private static final String MESSAGE = "Make sure the regex used here, which is vulnerable to %s runtime due to backtracking," +
    " cannot lead to denial of service%s.";
  private static final String JAVA8_MESSAGE = " or make sure the code is only run using Java 9 or later";
  private static final String EXP = "exponential";
  private static final String POLY = "polynomial";

  private final RedosFinder redosFinder = new RedosFinder() {
    @Override
    protected Optional<String> message(BacktrackingType backtrackingType, boolean b) {
      Field field = null;
      try {
        field = RedosFinder.class.getDeclaredField("regexContainsBackReference");
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      field.setAccessible(true); // Make the private field accessible
      boolean value = false; // Read the value
      try {
        value = field.getBoolean(redosFinder);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      boolean canBeOptimized = !value;
      boolean optimized = isJava9OrHigher() && canBeOptimized;
      switch (backtrackingType) {
        case ALWAYS_EXPONENTIAL:
          return Optional.of(String.format(MESSAGE, EXP, ""));
        case QUADRATIC_WHEN_OPTIMIZED:
          // We only suggest upgrading to Java 9+ when that would make the regex safe (i.e. linear runtime), not if it would
          // merely improve it from exponential to quadratic.
          return Optional.of(String.format(MESSAGE, optimized ? POLY : EXP, ""));
        case LINEAR_WHEN_OPTIMIZED:
          if (optimized) {
            return Optional.empty();
          } else {
            return Optional.of(String.format(MESSAGE, EXP, canBeOptimized ? JAVA8_MESSAGE : ""));
          }
        case ALWAYS_QUADRATIC:
          return Optional.of(String.format(MESSAGE, POLY, ""));
        case NO_ISSUE:
          return Optional.empty();
      }
      throw new IllegalStateException("This line is not actually reachable");
    }
  };

  private boolean isJava9OrHigher() {
    return context.getJavaVersion().isNotSet() || context.getJavaVersion().asInt() >= 9;
  }


  @Override
  protected void checkRegex(RegexParseResult regex, ExpressionTree methodInvocationOrAnnotation, MatchType matchType) {
    redosFinder.checkRegex(regex, matchType, (regexSyntaxElement, message, cost, secondaries) ->
      reportIssue(methodOrAnnotationName(methodInvocationOrAnnotation), message, null, Collections.emptyList()));
  }
}
