/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.security;

import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2115")
public class EmptyDatabasePasswordCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Add password protection to this database.";
  private static final int PASSWORD_ARGUMENT = 2;
  private static final int URL_ARGUMENT = 0;
  private static final Pattern EMPTY_PASSWORD_PATTERN = Pattern.compile(".*password\\s*=\\s*([&;\\)].*|$)");

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.sql.DriverManager")
      .names("getConnection")
      .withAnyParameters()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments args = mit.arguments();
    if (args.size() > 2) {
      checkEmptyValue(mit, args.get(PASSWORD_ARGUMENT));
    }
    if (args.size() == 1) {
      checkUrlContainsEmptyPassword(mit);
    }

  }

  private void checkEmptyValue(MethodInvocationTree mit, ExpressionTree expression) {
    ExpressionsHelper.ValueResolution<String> valueResolution =
      ExpressionsHelper.getConstantValueAsString(expression, "Empty password value.");
    String literal = valueResolution.value();
    if (literal != null && literal.trim().isEmpty()) {
      reportIssue(mit, MESSAGE, valueResolution.valuePath(), null);
    }
  }

  private void checkUrlContainsEmptyPassword(MethodInvocationTree mit) {
    ExpressionTree urlArgument = mit.arguments().get(URL_ARGUMENT);
    ExpressionsHelper.ValueResolution<String> valueResolution =
      ExpressionsHelper.getConstantValueAsString(urlArgument, "URL containing the empty password.");
    String url = valueResolution.value();
    if (url != null && urlContainsEmptyPassword(url)) {
      reportIssue(mit, MESSAGE, valueResolution.valuePath(), null);
    }
  }

  private static boolean urlContainsEmptyPassword(String url) {
    return EMPTY_PASSWORD_PATTERN.matcher(url).matches();
  }

}
