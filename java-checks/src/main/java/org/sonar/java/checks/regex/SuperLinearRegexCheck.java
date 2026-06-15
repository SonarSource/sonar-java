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
package org.sonar.java.checks.regex;

import java.util.Optional;
import org.sonar.check.Rule;

@Rule(key = "S8786")
public class SuperLinearRegexCheck extends AbstractRedosCheck {

  private static final String MESSAGE = "Simplify this regular expression to reduce its runtime, " +
    "as it has super-linear performance due to backtracking.";

  @Override
  Optional<String> buildMessage() {
    boolean optimized = isJava9OrHigher() && !regexContainsBackReference;
    return switch (foundBacktrackingType) {
      case ALWAYS_EXPONENTIAL -> Optional.empty();
      case QUADRATIC_WHEN_OPTIMIZED -> isJava9OrHigher() ? Optional.of(MESSAGE) : Optional.empty();
      case ALWAYS_QUADRATIC -> Optional.of(MESSAGE);
      case LINEAR_WHEN_OPTIMIZED -> optimized ? Optional.empty() : Optional.of(MESSAGE);
      case NO_ISSUE -> Optional.empty();
    };
  }

}
