/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2647")
public class BasicAuthCheck extends AbstractMethodDetection {

  private static final String LANG_STRING = "java.lang.String";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes("org.apache.http.message.AbstractHttpMessage").names("setHeader").withAnyParameters().build(),
      MethodMatchers.create()
        .ofSubTypes("org.apache.http.message.AbstractHttpMessage").names("addHeader").addParametersMatcher(LANG_STRING, LANG_STRING).build(),
      MethodMatchers.create()
        .ofSubTypes("org.apache.http.message.BasicHeader").constructor().addParametersMatcher(LANG_STRING, LANG_STRING).build(),
      MethodMatchers.create()
        .ofSubTypes("java.net.URLConnection").names("setRequestProperty").withAnyParameters().build(),
      MethodMatchers.create()
        .ofSubTypes("java.net.URLConnection").names("addRequestProperty").withAnyParameters().build()
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    checkArguments(mit.arguments());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    checkArguments(newClassTree.arguments());
  }

  private void checkArguments(Arguments arguments) {
    if ("Authorization".equals(ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value())) {
      ExpressionTree arg = mostLeft(arguments.get(1));
      String authentication = ExpressionsHelper.getConstantValueAsString(arg).value();
      if (authentication != null && authentication.startsWith("Basic")) {
        reportIssue(arg, "Use a more secure method than basic authentication.");
      }
    }
  }

  private static ExpressionTree mostLeft(ExpressionTree arg) {
    ExpressionTree res = ExpressionUtils.skipParentheses(arg);
    while (res.is(Tree.Kind.PLUS)) {
      res = ExpressionUtils.skipParentheses(((BinaryExpressionTree) res).leftOperand());
    }
    return res;
  }
}
