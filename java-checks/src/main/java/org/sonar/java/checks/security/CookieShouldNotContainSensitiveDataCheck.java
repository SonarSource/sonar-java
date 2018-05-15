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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;

import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S2255")
public class CookieShouldNotContainSensitiveDataCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "If the data stored in this cookie is sensitive, it should be stored internally in the user session.";

  private static class ClassName {
    private static final String SERVLET_COOKIE = "javax.servlet.http.Cookie";
    private static final String NET_HTTP_COOKIE = "java.net.HttpCookie";
    private static final String JAX_RS_COOKIE = "javax.ws.rs.core.Cookie";
  }

  private static final String CONSTRUCTOR = "<init>";
  private static final String SET_VALUE_METHOD = "setValue";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SERVLET_COOKIE)).name(CONSTRUCTOR).withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.NET_HTTP_COOKIE)).name(CONSTRUCTOR).withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_COOKIE)).name(CONSTRUCTOR).withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SERVLET_COOKIE)).name(SET_VALUE_METHOD).withAnyParameters(),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.NET_HTTP_COOKIE)).name(SET_VALUE_METHOD).withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    int valueParameterIndex = 1;
    if (newClassTree.arguments().size() <= valueParameterIndex) {
      return;
    }
    reportIssue(newClassTree.arguments().get(valueParameterIndex), MESSAGE);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodTree) {
    reportIssue(methodTree.methodSelect(), MESSAGE);
  }
}
