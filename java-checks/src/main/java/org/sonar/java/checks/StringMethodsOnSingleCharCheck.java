/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S3027")
public class StringMethodsOnSingleCharCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(STRING).name("indexOf").addParameter(STRING),
      MethodMatcher.create().typeDefinition(STRING).name("indexOf").addParameter(STRING).addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("lastIndexOf").addParameter(STRING),
      MethodMatcher.create().typeDefinition(STRING).name("lastIndexOf").addParameter(STRING).addParameter("int")
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree arg = mit.arguments().get(0);
    if (arg.is(Tree.Kind.STRING_LITERAL)) {
      String argValue = LiteralUtils.trimQuotes(((LiteralTree) arg).value());
      if (argValue.length() == 1 || isEscapedChar(argValue)) {
        reportIssue(arg, "Put single-quotes around '" + argValue + "' to use the faster \"" + mit.symbol().name() + "(char)\" method.");
      }
    }
  }

  private static boolean isEscapedChar(String argValue) {
    return argValue.length() == 2 && argValue.charAt(0) == '\\';
  }
}
