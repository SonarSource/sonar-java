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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S4825")
public class HttpRequestsHotspotCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcherCollection METHOD_MATCHER = MethodMatcherCollection.create(
    // === HttpClient Java 9/10 ===
    MethodMatcher.create().typeDefinition("jdk.incubator.http.HttpClient").name(NameCriteria.startsWith("send")).withAnyParameters(),

    // === HttpClient Java 11 ===
    MethodMatcher.create().typeDefinition("java.net.http.HttpClient").name(NameCriteria.startsWith("send")).withAnyParameters(),

    // === apache ===
    MethodMatcher.create().typeDefinition("org.apache.http.client.HttpClient").name("execute").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.apache.http.HttpClientConnection").name(NameCriteria.startsWith("send")).withAnyParameters(),

    // === google-http-java-client ===
    MethodMatcher.create().typeDefinition("com.google.api.client.http.HttpRequest").name(NameCriteria.startsWith("execute")).withAnyParameters());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (METHOD_MATCHER.anyMatch(mit)) {
        reportIssue(ExpressionUtils.methodName(mit), "Make sure that this http request is sent safely.");
      }
    } else {
      TypeCastTree castTree = (TypeCastTree) tree;
      if (castTree.type().symbolType().is("java.net.HttpURLConnection")) {
        reportIssue(tree, "Make sure that this http request is sent safely.");
      }
    }
  }

}
