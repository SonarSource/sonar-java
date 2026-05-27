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

import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S8700")
public class DateTimeDurationCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String TEMPORAL_TYPE = "java.time.temporal.Temporal";
  private static final String LOCALDATETIME_TYPE = "java.time.LocalDateTime";

  private static final Set<String> TIMEZONE_SENSITIVE_CHRONOUNITS = Set.of(
    "NANOS",
    "MICROS",
    "MILLIS",
    "SECONDS",
    "MINUTES",
    "HOURS",
    "HALF_DAYS",
    "DAYS"
  );

  private static final MethodMatchers DURATION_BETWEEN_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.Duration")
    .names("between")
    .addParametersMatcher(TEMPORAL_TYPE, TEMPORAL_TYPE)
    .build();

  private static final MethodMatchers CHRONOUNIT_BETWEEN_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.temporal.ChronoUnit")
    .names("between")
    .addParametersMatcher(TEMPORAL_TYPE, TEMPORAL_TYPE)
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(DURATION_BETWEEN_MATCHER, CHRONOUNIT_BETWEEN_MATCHER);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (CHRONOUNIT_BETWEEN_MATCHER.matches(mit) && !isMethodCalledOnChronoUnitTimeValue(mit)) {
      return;
    }
    ExpressionTree firstArg = mit.arguments().get(0);
    ExpressionTree secondArg = mit.arguments().get(1);
    if (firstArg.symbolType().is(LOCALDATETIME_TYPE) && secondArg.symbolType().is(LOCALDATETIME_TYPE)) {
      reportIssue(firstArg, secondArg, "Convert the arguments of this method call to time zone-aware types before computing a duration between them.");
    }
  }

  private static boolean isMethodCalledOnChronoUnitTimeValue(MethodInvocationTree mit) {
    return (mit.methodSelect() instanceof MemberSelectExpressionTree methodSelect) &&
      (methodSelect.expression() instanceof MemberSelectExpressionTree enumValue) &&
      TIMEZONE_SENSITIVE_CHRONOUNITS.stream().anyMatch(value -> enumValue.identifier().name().equals(value));
  }

}
