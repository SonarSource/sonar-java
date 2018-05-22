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
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S3330")
public class CookieHttpOnlyCheck extends InstanceShouldBeInitializedCorrectlyBase {

  // TODO move to static class and reuse
  private static class ClassName {
    private static final String SERVLET_COOKIE = "javax.servlet.http.Cookie";
    private static final String NET_HTTP_COOKIE = "java.net.HttpCookie";
    private static final String JAX_RS_COOKIE = "javax.ws.rs.core.Cookie";
    private static final String SHIRO_COOKIE = "org.apache.shiro.web.servlet.SimpleCookie";
  }

  @Override
  protected String getMessage() {
    return "Add the \"HttpOnly\" cookie attribute.";
  }

  @Override
  protected String getMethodName() {
    return "setHttpOnly";
  }

  @Override
  protected boolean methodArgumentsHaveExpectedValue(Arguments arguments) {
    ExpressionTree expressionTree = arguments.get(0);
    return LiteralUtils.isTrue(expressionTree);
  }

  @Override
  protected int getMethodArity() {
    return 1;
  }

  @Override
  protected List<String> getClasses() {
    return Arrays.asList(
      ClassName.SERVLET_COOKIE,
      ClassName.NET_HTTP_COOKIE,
      ClassName.JAX_RS_COOKIE,
      ClassName.SHIRO_COOKIE);
  }
}
