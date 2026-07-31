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
package org.sonar.java.checks.helpers;

import org.sonar.plugins.java.api.semantic.MethodMatchers;

/**
 * Shared logging-framework {@link MethodMatchers} used across multiple rules.
 *
 * <p>Covers the three most common Java logging frameworks:
 * <ul>
 *   <li>SLF4J ({@code org.slf4j.Logger})</li>
 *   <li>Java Util Logging ({@code java.util.logging.Logger})</li>
 *   <li>Log4j 2 ({@code org.apache.logging.log4j.Logger})</li>
 * </ul>
 *
 * <p>Each matcher uses {@code withAnyParameters()} intentionally: rules that import
 * these constants typically only need to detect <em>that</em> a call is a logging call,
 * not which specific overload is used.
 */
public final class LoggingMatchers {

  /** SLF4J log-level methods on {@code org.slf4j.Logger} and its subtypes. */
  public static final MethodMatchers SLF4J_LOG_METHODS = MethodMatchers.create()
    .ofSubTypes("org.slf4j.Logger")
    .names("trace", "debug", "info", "warn", "error")
    .withAnyParameters()
    .build();

  /** Java Util Logging named-level and {@code log(Level, ...)} methods on {@code java.util.logging.Logger}. */
  public static final MethodMatchers JUL_LOG_METHODS = MethodMatchers.create()
    .ofTypes("java.util.logging.Logger")
    .names("severe", "warning", "info", "config", "fine", "finer", "finest", "log")
    .withAnyParameters()
    .build();

  /** Log4j 2 log-level and {@code log(...)} methods on {@code org.apache.logging.log4j.Logger} and its subtypes. */
  public static final MethodMatchers LOG4J_LOG_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("org.apache.logging.log4j.Logger")
      .names("trace", "debug", "info", "warn", "error", "fatal")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("org.apache.logging.log4j.Logger")
      .names("log")
      .withAnyParameters()
      .build());

  /** Combined matcher for all three frameworks — use when any logging call must be detected. */
  public static final MethodMatchers LOG_METHODS = MethodMatchers.or(SLF4J_LOG_METHODS, JUL_LOG_METHODS, LOG4J_LOG_METHODS);

  private LoggingMatchers() {
    // utility class
  }
}
