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

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S8695")
public class SimpleTemporalInstantiationCheck extends AbstractMethodDetection {

  private static final MethodMatchers NOW_METHODS_WITH_ZONE_ID = MethodMatchers.create()
    .ofTypes("java.time.ZonedDateTime", "java.time.OffsetDateTime", "java.time.LocalDate", "java.time.YearMonth")
    .names("now")
    .addParametersMatcher("java.time.ZoneId")
    .build();

  private static final MethodMatchers AT_ZONE_METHOD = MethodMatchers.create()
    .ofTypes("java.time.Instant")
    .names("atZone")
    .addParametersMatcher("java.time.ZoneId")
    .build();

  private static final MethodMatchers INSTANT_NOW = MethodMatchers.create()
    .ofTypes("java.time.Instant")
    .names("now")
    .addWithoutParametersMatcher()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.time.LocalDate", "java.time.LocalTime", "java.time.YearMonth", "java.time.Year")
      .names("from")
      .addParametersMatcher("java.time.temporal.TemporalAccessor")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    if (arguments.size() == 1 && arguments.get(0) instanceof MethodInvocationTree method && isNonCompliantMethod(method)) {
      String argument = method.arguments().size() == 1 ? QuickFixHelper.contentForTree(method.arguments().get(0), context) : "";
      String replacement = "now(" + argument + ")";
      IdentifierTree methodName = ExpressionUtils.methodName(mit);
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(methodName)
        .withMessage("Replace \"from(TemporalAccessor)\" with \"now(ZoneId)\".")
        .withQuickFix(JavaQuickFix.newQuickFix("Replace with %s", replacement)
          .addTextEdit(JavaTextEdit.replaceTextSpan(textSpanBetween(methodName, true, mit.arguments(), true), replacement))::build)
        .report();
    }
  }

  private static boolean isNonCompliantMethod(MethodInvocationTree method) {
    if (method.methodSymbol().isStatic()) {
      return INSTANT_NOW.matches(method) || NOW_METHODS_WITH_ZONE_ID.matches(method);
    } else {
      return AT_ZONE_METHOD.matches(method) &&
        method.methodSelect() instanceof MemberSelectExpressionTree methodSelect &&
        methodSelect.expression() instanceof MethodInvocationTree callee &&
        callee.methodSymbol().isStatic() && INSTANT_NOW.matches(callee);
    }
  }

}
