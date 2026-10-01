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
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9363")
public class TimeZoneIdCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Change this invalid time zone ID; an unrecognized identifier makes TimeZone.getTimeZone(String) return GMT with no error.";

  private static final MethodMatchers TIME_ZONE_GET_TIME_ZONE = MethodMatchers.create()
    .ofTypes("java.util.TimeZone")
    .names("getTimeZone")
    .addParametersMatcher("java.lang.String")
    .build();

  private static final Set<String> AVAILABLE_ZONE_IDS = Set.copyOf(Arrays.asList(TimeZone.getAvailableIDs()));

  private static final Pattern CUSTOM_GMT_PRE_JAVA19 = Pattern.compile(
    "^GMT[+-](?:[01]?\\d|2[0-3])(?::[0-5]\\d|[0-5]\\d)?$"
  );

  private static final Pattern CUSTOM_GMT_JAVA19_PLUS = Pattern.compile(
    "^GMT[+-](?:[01]?\\d|2[0-3])(?::[0-5]\\d(?::[0-5]\\d)?|[0-5]\\d)?$"
  );

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return TIME_ZONE_GET_TIME_ZONE;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!mit.methodSymbol().isStatic() || mit.arguments().isEmpty()) {
      return;
    }
    ExpressionTree argument = mit.arguments().get(0);
    Optional<String> constantValue = argument.asConstant(String.class);
    if (constantValue.isEmpty()) {
      return;
    }
    String zoneId = constantValue.get();
    if (isValidZoneId(zoneId)) {
      return;
    }

    InternalJavaIssueBuilder issueBuilder = QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(argument)
      .withMessage(MESSAGE);

    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      String proposed = zoneId.replace(' ', '_');
      if (!proposed.equals(zoneId) && AVAILABLE_ZONE_IDS.contains(proposed)) {
        issueBuilder.withQuickFix(() -> JavaQuickFix.newQuickFix("Replace spaces with underscores")
          .addTextEdit(JavaTextEdit.replaceTree(argument, "\"" + proposed + "\""))
          .build());
      }
    }

    issueBuilder.report();
  }

  private boolean isValidZoneId(String zoneId) {
    if (AVAILABLE_ZONE_IDS.contains(zoneId)) {
      return true;
    }
    boolean allowsSeconds = context.getJavaVersion().isNotSet() || context.getJavaVersion().isJava19Compatible();
    Pattern customGmtPattern = allowsSeconds ? CUSTOM_GMT_JAVA19_PLUS : CUSTOM_GMT_PRE_JAVA19;
    return customGmtPattern.matcher(zoneId).matches();
  }
}
