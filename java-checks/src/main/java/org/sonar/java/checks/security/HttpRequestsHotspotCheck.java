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
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S4825")
public class HttpRequestsHotspotCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition("java.net.URL").name("openConnection").withAnyParameters(),

      // === HttpClient Java 9/10 ===
      MethodMatcher.create().typeDefinition("jdk.incubator.http.HttpClient").name(NameCriteria.startsWith("send")).withAnyParameters(),

      // === HttpClient Java 11 ===
      MethodMatcher.create().typeDefinition("java.net.http.HttpClient").name(NameCriteria.startsWith("send")).withAnyParameters(),

      // === apache ===
      MethodMatcher.create().typeDefinition("org.apache.http.client.HttpClient").name("execute").withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.apache.http.HttpClientConnection").name(NameCriteria.startsWith("send")).withAnyParameters(),

      // === google-http-java-client ===
      MethodMatcher.create().typeDefinition("com.google.api.client.http.HttpRequest").name(NameCriteria.startsWith("execute")).withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!"openConnection".equals(mit.symbol().name()) || isCastToHttpUrlConnection(mit.parent())) {
      reportIssue(ExpressionUtils.methodName(mit), "Make sure that this http request is sent safely.");
    }
  }

  private static boolean isCastToHttpUrlConnection(Tree parent) {
    if (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      return isCastToHttpUrlConnection(parent.parent());
    } else if (parent.is(Tree.Kind.TYPE_CAST)) {
      return ((TypeCastTree) parent).type().symbolType().is("java.net.HttpURLConnection");
    }
    return false;
  }

}
