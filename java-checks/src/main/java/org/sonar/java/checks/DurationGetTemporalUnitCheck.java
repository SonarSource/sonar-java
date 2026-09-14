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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S9393")
public class DurationGetTemporalUnitCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String DURATION = "java.time.Duration";
  private static final String TEMPORAL_UNIT = "java.time.temporal.TemporalUnit";
  private static final String CHRONO_UNIT = "java.time.temporal.ChronoUnit";

  private static final Set<String> SUPPORTED_UNITS = Set.of(
    "SECONDS",
    "NANOS"
  );

  private static final MethodMatchers DURATION_GET_MATCHER = MethodMatchers.create()
    .ofTypes(DURATION)
    .names("get")
    .addParametersMatcher(TEMPORAL_UNIT)
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return DURATION_GET_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (context.getSemanticModel() == null) {
      return;
    }
    ExpressionTree argument = mit.arguments().get(0);
    if (isUnsupportedTemporalUnit(argument)) {
      reportIssue(argument, "\"Duration.get()\" only supports \"SECONDS\" and \"NANOS\"; use dedicated conversion methods instead.");
    }
  }

  private static boolean isUnsupportedTemporalUnit(ExpressionTree argument) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(argument);
    IdentifierTree identifier = null;
    if (expr instanceof IdentifierTree id) {
      identifier = id;
    } else if (expr instanceof MemberSelectExpressionTree memberSelect) {
      identifier = memberSelect.identifier();
    }
    if (identifier == null) {
      return false;
    }
    Symbol symbol = identifier.symbol();
    if (symbol.isUnknown() || !symbol.isVariableSymbol() || !symbol.isEnum()) {
      return false;
    }
    Symbol owner = symbol.owner();
    if (owner == null || owner.isUnknown()) {
      return false;
    }
    Type ownerType = owner.type();
    if (ownerType.is(CHRONO_UNIT)) {
      return !SUPPORTED_UNITS.contains(symbol.name());
    }
    return ownerType.isSubtypeOf(TEMPORAL_UNIT);
  }
}
