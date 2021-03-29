/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
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

  private static final String SET_VALUE_METHOD = "setValue";
  private static final String WITH_VALUE_METHOD = "withValue";
  private static final String BUILDER_METHOD = "builder";
  private static final String JAVA_LANG_STRING = "java.lang.String";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      // setters
      MethodMatchers.create()
        .ofSubTypes(ClassName.SERVLET_COOKIE, ClassName.NET_HTTP_COOKIE, ClassName.SHIRO_COOKIE)
        .names(SET_VALUE_METHOD)
        .addParametersMatcher(JAVA_LANG_STRING)
        .build(),
      // constructors
      MethodMatchers.create()
        .ofSubTypes(ClassName.SERVLET_COOKIE, ClassName.NET_HTTP_COOKIE, ClassName.SHIRO_COOKIE)
        .constructor()
        .withAnyParameters()
        .build(),
      // javax.ws.rs.core.NewCookie is a subtype of JAX_RS_COOKIE
      MethodMatchers.create().ofSubTypes(ClassName.JAX_RS_COOKIE).constructor().withAnyParameters().build(),
      MethodMatchers.create().ofTypes(ClassName.PLAY_COOKIE).names(BUILDER_METHOD).addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING).build(),
      MethodMatchers.create().ofSubTypes(ClassName.PLAY_COOKIE_BUILDER).names(WITH_VALUE_METHOD).addParametersMatcher(JAVA_LANG_STRING).build());
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

  private static boolean isNotNullOrWhitespace(ExpressionTree tree) {
    return !tree.is(Tree.Kind.NULL_LITERAL)
      && !tree.asConstant(String.class).filter(StringUtils::isBlank).isPresent();
  }
}
