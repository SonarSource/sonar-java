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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3330")
public class CookieHttpOnlyCheck extends InstanceShouldBeInitializedCorrectlyBase {

  private static final String CONSTRUCTOR = "<init>";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_DATE = "java.util.Date";
  private static final String INT = "int";
  private static final String BOOLEAN = "boolean";

  private static final class ClassName {
    private static final String SERVLET_COOKIE = "javax.servlet.http.Cookie";
    private static final String NET_HTTP_COOKIE = "java.net.HttpCookie";
    private static final String JAX_RS_COOKIE = "javax.ws.rs.core.Cookie";
    private static final String JAX_RS_NEW_COOKIE = "javax.ws.rs.core.NewCookie";
    private static final String SHIRO_COOKIE = "org.apache.shiro.web.servlet.SimpleCookie";
  }

  private static List<MethodMatcher> constructorsWithHttpOnlyParameter() {
    return Arrays.asList(
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_NEW_COOKIE)).name(CONSTRUCTOR)
          .parameters(ClassName.JAX_RS_COOKIE, JAVA_LANG_STRING, INT, JAVA_UTIL_DATE, BOOLEAN, BOOLEAN),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_NEW_COOKIE)).name(CONSTRUCTOR)
          .parameters(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, JAVA_LANG_STRING, INT, JAVA_UTIL_DATE, BOOLEAN, BOOLEAN),
        MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_NEW_COOKIE)).name(CONSTRUCTOR)
          .parameters(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, BOOLEAN, BOOLEAN));
  }

  private static final List<MethodMatcher> constructorsWithGoodDefault() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SHIRO_COOKIE)).name(CONSTRUCTOR).withoutParameter(),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SHIRO_COOKIE)).name(CONSTRUCTOR).parameters(JAVA_LANG_STRING));
  }

  @Override
  protected String getMessage() {
    return "Add the \"HttpOnly\" cookie attribute.";
  }

  @Override
  protected boolean constructorInitializesCorrectly(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    if (initializer != null && initializer.is(Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) initializer;
      if (constructorsWithHttpOnlyParameter().stream().anyMatch(matcher -> matcher.matches(newClassTree))) {
        Arguments arguments = newClassTree.arguments();
        ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
        return LiteralUtils.isTrue(lastArgument);
      } else {
        return constructorsWithGoodDefault().stream().anyMatch(matcher -> matcher.matches(newClassTree));
      }
    }
    return false;
  }

  @Override
  protected String getSetterName() {
    return "setHttpOnly";
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
