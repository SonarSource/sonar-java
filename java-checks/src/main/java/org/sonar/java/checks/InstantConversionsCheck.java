/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Arrays;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S8220")
public class InstantConversionsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String INSTANT = "java.time.Instant";
  private static final String TEMPORAL_ACCESSOR = "java.time.temporal.TemporalAccessor";

  private static final String[] LOCAL_DATE_TIME_TYPES = {
    "java.time.LocalDate",
    "java.time.LocalTime",
    "java.time.LocalDateTime",
    "java.time.DayOfWeek",
    "java.time.Month",
    "java.time.MonthDay",
    "java.time.Year",
    "java.time.YearMonth"
  };

  private static final String[] ZONE_AWARE_DATE_TIME_TYPES = {
    "java.time.ZonedDateTime",
    "java.time.OffsetDateTime",
    "java.time.OffsetTime"
  };

  private static final MethodMatchers INSTANT_FROM_MATCHER = MethodMatchers.create()
    .ofTypes(INSTANT)
    .names("from")
    .addParametersMatcher(TEMPORAL_ACCESSOR)
    .build();

  private static final MethodMatchers LOCAL_DATE_TIME_FROM_MATCHER = MethodMatchers.create()
    .ofTypes(LOCAL_DATE_TIME_TYPES)
    .names("from")
    .addParametersMatcher(TEMPORAL_ACCESSOR)
    .build();

  private static final MethodMatchers ZONE_AWARE_DATE_TIME_FROM_MATCHER = MethodMatchers.create()
    .ofTypes(ZONE_AWARE_DATE_TIME_TYPES)
    .names("from")
    .addParametersMatcher(TEMPORAL_ACCESSOR)
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(INSTANT_FROM_MATCHER, LOCAL_DATE_TIME_FROM_MATCHER, ZONE_AWARE_DATE_TIME_FROM_MATCHER);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree argument = mit.arguments().get(0);
    Type argumentType = skipParenthesesAndCasts(argument).symbolType();
    if (INSTANT_FROM_MATCHER.matches(mit) && isLocalDateOrTime(argumentType)) {
      reportIssue(argument, "Provide explicit timezone information when converting from a local date/time type to an instant.");
    } else if (LOCAL_DATE_TIME_FROM_MATCHER.matches(mit) && argumentType.is(INSTANT)) {
      reportIssue(argument, "Provide explicit timezone information when converting from an instant to a local date/time type.");
    } else if (ZONE_AWARE_DATE_TIME_FROM_MATCHER.matches(mit) && (argumentType.is(INSTANT) || isLocalDateOrTime(argumentType))) {
      reportIssue(argument, "Provide explicit timezone information when converting from a local date/time type or an instant to a timezone aware type.");
    }
  }

  private static boolean isLocalDateOrTime(Type type) {
    return Arrays.stream(LOCAL_DATE_TIME_TYPES).anyMatch(type::is);
  }

  private static ExpressionTree skipParenthesesAndCasts(ExpressionTree expression) {
    ExpressionTree result = expression;
    while (true) {
      if (result instanceof ParenthesizedTree parenthesizedTree) {
        result = parenthesizedTree.expression();
      } else if (result instanceof TypeCastTree typeCastTree) {
        result = typeCastTree.expression();
      } else {
        return result;
      }
    }
  }
}
