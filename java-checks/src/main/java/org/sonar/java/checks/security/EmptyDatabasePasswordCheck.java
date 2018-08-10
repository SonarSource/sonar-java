/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.helpers.IdentifierUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2115")
public class EmptyDatabasePasswordCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Add password protection to this database.";
  private static final int PASSWORD_ARGUMENT = 2;
  private static final int URL_ARGUMENT = 0;
  private static final Pattern EMPTY_PASSWORD_PATTERN = Pattern.compile(".*password\\s*=\\s*([&;].*|$)");
  private static final List<MethodMatcher> METHOD_MATCHERS = Collections.singletonList(
    MethodMatcher.create().typeDefinition("java.sql.DriverManager").name("getConnection").withAnyParameters());


  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return METHOD_MATCHERS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments args = mit.arguments();
    if (args.size() > 2 && hasEmptyValue(args.get(PASSWORD_ARGUMENT))) {
      reportIssue(mit, MESSAGE);
    }
    if (args.size() == 1) {
      checkForUrlConnection(mit);
    }
  }

  private static boolean hasEmptyValue(ExpressionTree expression) {
    String literal = IdentifierUtils.getValue(expression, ConstantUtils::resolveAsStringConstant);
    return literal != null && literal.trim().isEmpty();
  }

  private void checkForUrlConnection(MethodInvocationTree mit) {
    ExpressionTree urlArgument = mit.arguments().get(URL_ARGUMENT);
    String url = IdentifierUtils.getValue(urlArgument, ConstantUtils::resolveAsStringConstant);
    if (url != null && (!url.contains("password") || EMPTY_PASSWORD_PATTERN.matcher(url).matches())) {
      reportIssue(mit, MESSAGE);
    }

  }

}
