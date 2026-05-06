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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S8220")
public class InstantConversionsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String INSTANT = "java.time.Instant";
  private static final String TEMPORAL_ACCESSOR = "java.time.temporal.TemporalAccessor";

  private static final String[] LOCAL_DATE_AND_TIME_TYPES = {
    "java.time.LocalDate",
    "java.time.LocalTime",
    "java.time.LocalDateTime",
    "java.time.DayOfWeek",
    "java.time.Month",
    "java.time.MonthDay",
    "java.time.Year",
    "java.time.YearMonth"
  };

  private static final MethodMatchers INSTANT_FROM_MATCHER = MethodMatchers.create()
    .ofTypes(INSTANT)
    .names("from")
    .addParametersMatcher(TEMPORAL_ACCESSOR)
    .build();

  private static final MethodMatchers LOCAL_DATE_AND_TIME_FROM_MATCHER = MethodMatchers.create()
    .ofTypes(LOCAL_DATE_AND_TIME_TYPES)
    .names("from")
    .addParametersMatcher(TEMPORAL_ACCESSOR)
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(INSTANT_FROM_MATCHER, LOCAL_DATE_AND_TIME_FROM_MATCHER);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Type sourceType = skipParenthesesAndCasts(mit.arguments().get(0)).symbolType();
    if ((INSTANT_FROM_MATCHER.matches(mit) && isLocalDateOrTime(sourceType)) || (LOCAL_DATE_AND_TIME_FROM_MATCHER.matches(mit) && sourceType.is(INSTANT))) {
      reportIssue(mit.arguments().get(0), "Provide explicit timezone information when converting between local date/time types and \"Instant\".");
    }
  }

  private static boolean isLocalDateOrTime(Type type) {
    return Arrays.stream(LOCAL_DATE_AND_TIME_TYPES).anyMatch(type::is);
  }

  private static ExpressionTree skipParenthesesAndCasts(ExpressionTree expression) {
    ExpressionTree result = expression;
    while (true) {
      if (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        result = ((ParenthesizedTree) result).expression();
      } else if (result.is(Tree.Kind.TYPE_CAST)) {
        result = ((TypeCastTree) result).expression();
      } else {
        return result;
      }
    }
  }
}
