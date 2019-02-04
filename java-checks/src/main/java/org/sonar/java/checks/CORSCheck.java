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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5122")
public class CORSCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher SET_HEADER_MATCHER = MethodMatcher.create().typeDefinition("javax.servlet.http.HttpServletResponse").name("setHeader").withAnyParameters();
  private static final Set<String> HTTP_HEADERS = new HashSet<>(Arrays.asList(
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Credentials",
    "Access-Control-Expose-Headers",
    "Access-Control-Max-Age",
    "Access-Control-Allow-Methods",
    "Access-Control-Allow-Headers"));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION) && SET_HEADER_MATCHER.matches(((MethodInvocationTree) tree))) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      String constantCORS = ConstantUtils.resolveAsStringConstant(mit.arguments().get(0));
      if (HTTP_HEADERS.contains(constantCORS)) {
        reportTree(mit.methodSelect());
      }
    } else if (tree.is(Tree.Kind.ANNOTATION) && ((AnnotationTree) tree).symbolType().is("org.springframework.web.bind.annotation.CrossOrigin")) {
      reportTree(((AnnotationTree) tree).annotationType());
    }
  }

  private void reportTree(Tree tree) {
    reportIssue(tree, "Make sure that enabling CORS is safe here.");
  }
}
