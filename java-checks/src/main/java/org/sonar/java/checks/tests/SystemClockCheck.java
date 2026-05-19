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
package org.sonar.java.checks.tests;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S8692")
public class SystemClockCheck extends AbstractMethodDetection {

  private static final MethodMatchers SYSTEM_CLOCK_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.time.LocalDate",
        "java.time.LocalTime",
        "java.time.LocalDateTime",
        "java.time.MonthDay",
        "java.time.Year",
        "java.time.YearMonth",
        "java.time.ZonedDateTime",
        "java.time.OffsetDateTime",
        "java.time.OffsetTime",
        "java.time.Instant")
      .names("now")
      .addWithoutParametersMatcher()
      .addParametersMatcher("java.time.ZoneId")
      .build(),
    MethodMatchers.create()
      .ofTypes("java.time.Clock")
      .names("systemUTC", "systemDefaultZone", "system")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.lang.System")
      .names("currentTimeMillis")
      .addWithoutParametersMatcher()
      .build()
  );

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return SYSTEM_CLOCK_MATCHERS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(mit, "Do not use the system clock in tests.");
  }

}
