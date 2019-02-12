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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2639")
public class InappropriateRegexpCheck extends AbstractMethodDetection {

  private static final String INAPPROPRIATE_REGEXPS = "\\.|\\|";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition("java.lang.String").name("replaceAll").withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.lang.String").name("replaceFirst").withAnyParameters()
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArg = mit.arguments().get(0);
    if (isInappropriateRegexpStringLiteral(firstArg) || isFileSeparator(firstArg)) {
      reportIssue(firstArg, "Correct this regular expression.");
    }
  }

  private static boolean isInappropriateRegexpStringLiteral(ExpressionTree firstArg) {
    if (firstArg.is(Tree.Kind.STRING_LITERAL)) {
      String regexp = LiteralUtils.trimQuotes(((LiteralTree) firstArg).value());
      return regexp.matches(INAPPROPRIATE_REGEXPS);
    }
    return false;
  }

  private static boolean isFileSeparator(ExpressionTree firstArg) {
    if (firstArg.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) firstArg;
      return "separator".equals(mse.identifier().name()) && mse.expression().symbolType().is("java.io.File");
    }
    return false;
  }

}
