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
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S8218")
public class UnsupportedChronoUnitWithInstantCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String INSTANT = "java.time.Instant";
  private static final String TEMPORAL = "java.time.temporal.Temporal";
  private static final String TEMPORAL_UNIT = "java.time.temporal.TemporalUnit";
  private static final String CHRONO_UNIT = "java.time.temporal.ChronoUnit";

  private static final Set<String> UNSUPPORTED_UNITS = Set.of(
    "WEEKS",
    "MONTHS",
    "YEARS",
    "DECADES",
    "CENTURIES",
    "MILLENNIA",
    "ERAS",
    "FOREVER"
  );

  private static final MethodMatchers MATCHERS = MethodMatchers.create()
    .ofTypes(INSTANT)
    .names("plus", "minus")
    .addParametersMatcher("long", TEMPORAL_UNIT)
    .build();

  private static final MethodMatchers UNTIL_MATCHER = MethodMatchers.create()
    .ofTypes(INSTANT)
    .names("until")
    .addParametersMatcher(TEMPORAL, TEMPORAL_UNIT)
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(MATCHERS, UNTIL_MATCHER);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (context.getSemanticModel() == null) {
      return;
    }
    ExpressionTree argument = mit.arguments().get(1);
    Symbol symbol = referencedSymbol(ExpressionUtils.skipParentheses(argument));
    if (isChronoUnitConstant(symbol) && UNSUPPORTED_UNITS.contains(symbol.name())) {
      reportIssue(argument, String.format("\"%s\" is unsupported by Instant and causes an UnsupportedTemporalTypeException.", symbol.name()));
    }
  }

  private static @Nullable Symbol referencedSymbol(ExpressionTree argument) {
    if (argument instanceof IdentifierTree identifier) {
      return identifier.symbol();
    }
    if (argument instanceof MemberSelectExpressionTree memberSelect) {
      return memberSelect.identifier().symbol();
    }
    return null;
  }

  private static boolean isChronoUnitConstant(@Nullable Symbol symbol) {
    if (symbol == null || symbol.isUnknown() || !symbol.isVariableSymbol() || !symbol.isEnum()) {
      return false;
    }
    Symbol owner = symbol.owner();
    return owner != null && !owner.isUnknown() && owner.type().is(CHRONO_UNIT);
  }
}
