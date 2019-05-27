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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
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

  private static final MethodMatcher ADD_ALLOWED_ORIGIN = MethodMatcher.create().typeDefinition("org.springframework.web.cors.CorsConfiguration")
    .name("addAllowedOrigin").withAnyParameters();
  private static final MethodMatcher APPLY_PERMIT_DEFAULT_VALUES = MethodMatcher.create().typeDefinition("org.springframework.web.cors.CorsConfiguration")
    .name("applyPermitDefaultValues").withAnyParameters();
  public static final String MESSAGE = "Make sure that enabling CORS is safe here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      checkMethod(tree);
    } else if (tree.is(Tree.Kind.ANNOTATION) && ((AnnotationTree) tree).symbolType().is("org.springframework.web.bind.annotation.CrossOrigin")) {
      reportTree(((AnnotationTree) tree).annotationType());
    }
  }

  private void checkMethod(Tree tree) {
    MethodInvocationVisitor visitor = new MethodInvocationVisitor();
    tree.accept(visitor);
    if (!visitor.addAllowedOrigin.isEmpty() && !visitor.applyPermit.isEmpty()) {
      visitor.addAllowedOrigin.forEach(mit -> {
        List<Location> locations = visitor.applyPermit.stream()
          .map(t -> new Location(MESSAGE, t))
          .collect(Collectors.toList());
        reportIssue(mit.methodSelect(), MESSAGE, locations, null);
      });
    } else {
      visitor.addAllowedOrigin.forEach(this::reportTree);
      visitor.applyPermit.forEach(this::reportTree);
    }
  }

  private void reportTree(Tree tree) {
    reportIssue(tree, MESSAGE);
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {
    List<MethodInvocationTree> addAllowedOrigin = new ArrayList<>();
    List<MethodInvocationTree> applyPermit = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (SET_HEADER_MATCHER.matches(mit)) {
        String constantCORS = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
        if (HTTP_HEADERS.contains(constantCORS)) {
          reportTree(mit.methodSelect());
        }
      } else if (APPLY_PERMIT_DEFAULT_VALUES.matches(mit)) {
        applyPermit.add(mit);
      } else if (ADD_ALLOWED_ORIGIN.matches(mit)) {
        addAllowedOrigin.add(mit);
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // cut the visit
    }
  }
}
