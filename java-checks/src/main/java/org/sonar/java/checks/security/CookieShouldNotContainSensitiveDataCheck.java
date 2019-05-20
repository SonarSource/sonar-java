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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2255")
public class CookieShouldNotContainSensitiveDataCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Make sure that this cookie is used safely.";

  private static class ClassName {
    private static final String SERVLET_COOKIE = "javax.servlet.http.Cookie";
    private static final String NET_HTTP_COOKIE = "java.net.HttpCookie";
    private static final String JAX_RS_COOKIE = "javax.ws.rs.core.Cookie";
    private static final String SHIRO_COOKIE = "org.apache.shiro.web.servlet.SimpleCookie";
    private static final String PLAY_COOKIE = "play.mvc.Http$Cookie";
    private static final String PLAY_COOKIE_BUILDER = "play.mvc.Http$CookieBuilder";
  }

  private static final List<String> COOKIE_ARGUMENT_TYPES = Arrays.asList(
      ClassName.SERVLET_COOKIE,
      ClassName.JAX_RS_COOKIE,
      "org.apache.shiro.web.servlet.Cookie"
  );

  private static final String CONSTRUCTOR = "<init>";
  private static final String SET_VALUE_METHOD = "setValue";
  private static final String WITH_VALUE_METHOD = "withValue";
  private static final String BUILDER_METHOD = "builder";
  private static final String JAVA_LANG_STRING = "java.lang.String";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      // setters
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SERVLET_COOKIE)).name(SET_VALUE_METHOD).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.NET_HTTP_COOKIE)).name(SET_VALUE_METHOD).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SHIRO_COOKIE)).name(SET_VALUE_METHOD).parameters(JAVA_LANG_STRING),
      // constructors
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SERVLET_COOKIE)).name(CONSTRUCTOR).withAnyParameters(),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.NET_HTTP_COOKIE)).name(CONSTRUCTOR).withAnyParameters(),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.SHIRO_COOKIE)).name(CONSTRUCTOR).withAnyParameters(),
      // javax.ws.rs.core.NewCookie is a subtype of JAX_RS_COOKIE
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.JAX_RS_COOKIE)).name(CONSTRUCTOR).withAnyParameters(),
      MethodMatcher.create().typeDefinition(ClassName.PLAY_COOKIE).name(BUILDER_METHOD).parameters(JAVA_LANG_STRING, JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(ClassName.PLAY_COOKIE_BUILDER)).name(WITH_VALUE_METHOD).parameters(JAVA_LANG_STRING));
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_REFERENCE);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodTree) {
    if (methodTree.symbol().name().equals(BUILDER_METHOD)) {
      if (secondArgumentIsValue(methodTree.arguments())) {
        reportIssue(methodTree.arguments().get(1), MESSAGE);
      }
    } else if (isNotNullOrWhitespace(methodTree.arguments().get(0))) {
      reportIssue(methodTree.arguments().get(0), MESSAGE);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (firstArgumentIsCookie(newClassTree.arguments())) {
      reportIssue(newClassTree.arguments().get(0), MESSAGE);
    } else if (secondArgumentIsValue(newClassTree.arguments())) {
      reportIssue(newClassTree.arguments().get(1), MESSAGE);
    }
  }

  private static boolean firstArgumentIsCookie(Arguments arguments) {
    if (arguments.isEmpty()) {
      return false;
    }
    ExpressionTree firstArgument = arguments.get(0);
    return COOKIE_ARGUMENT_TYPES.stream().anyMatch(type -> firstArgument.symbolType().isSubtypeOf(type));
  }

  private static boolean secondArgumentIsValue(Arguments arguments) {
    if (arguments.size() < 2) {
      return false;
    }
    ExpressionTree secondArgument = arguments.get(1);
    return secondArgument.symbolType().isSubtypeOf(JAVA_LANG_STRING) && isNotNullOrWhitespace(secondArgument);
  }

  private static boolean isNotNullOrWhitespace(Tree tree) {
    return !tree.is(Tree.Kind.NULL_LITERAL)
        && !(tree.is(Tree.Kind.STRING_LITERAL) && StringUtils.isBlank(LiteralUtils.trimQuotes(((LiteralTree) tree).value())));
  }
}
