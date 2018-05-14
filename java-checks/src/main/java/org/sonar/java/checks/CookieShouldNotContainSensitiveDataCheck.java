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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;

import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2255")
public class CookieShouldNotContainSensitiveDataCheck extends AbstractMethodDetection {

  private static final String SERVLET_COOKIE = "javax.servlet.http.Cookie";
  private static final String NET_HTTP_COOKIE = "java.net.HttpCookie";
  private static final String JAX_RS_COOKIE = "javax.ws.rs.core.Cookie";

  private static final int VALUE_PARAM_INDEX = 1;

  private static final String MESSAGE = "If the data stored in this cookie is sensitive, it should be stored internally in the user session.";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(SERVLET_COOKIE)).name("<init>").withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(SERVLET_COOKIE)).name("setValue").withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(NET_HTTP_COOKIE)).name("<init>").withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(NET_HTTP_COOKIE)).name("setValue").withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(JAX_RS_COOKIE)).name("<init>").withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (newClassTree.arguments().size() <= VALUE_PARAM_INDEX) {
      return;
    }
    if (hasNameValueParameters(newClassTree)) {
      reportIssue(newClassTree.arguments().get(VALUE_PARAM_INDEX), MESSAGE);
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodTree) {
    if (methodTree.arguments().size() != 1) {
      return;
    }
    reportIssue(methodTree.methodSelect(), MESSAGE);
  }

  // this treats the corner case javax.ws.rs.core.NewCookie
  private boolean hasNameValueParameters(NewClassTree newClassTree) {
    return newClassTree.arguments().get(0).is(Kind.STRING_LITERAL);
  }
}
