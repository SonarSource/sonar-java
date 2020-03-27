/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5122")
public class CORSCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers SET_ADD_HEADER_MATCHER = MethodMatchers.create()
    .ofTypes("javax.servlet.http.HttpServletResponse")
    .names("setHeader", "addHeader")
    .withAnyParameters()
    .build();

  private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "access-control-allow-origin";
  private static final Set<String> ANNOTATION_ORIGINS_KEY_ALIAS = ImmutableSet.of("origins", "value");

  private static final MethodMatchers ADD_ALLOWED_ORIGIN_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("org.springframework.web.cors.CorsConfiguration")
      .names("addAllowedOrigin")
      .withAnyParameters()
      .build(),
    MethodMatchers.create().ofTypes("org.springframework.web.servlet.config.annotation.CorsRegistration")
      .names("allowedOrigins")
      .withAnyParameters()
      .build());

  private static final MethodMatchers APPLY_PERMIT_DEFAULT_VALUES = MethodMatchers.create()
    .ofTypes("org.springframework.web.cors.CorsConfiguration")
    .names("applyPermitDefaultValues")
    .withAnyParameters()
    .build();

  public static final String MESSAGE = "Make sure that enabling CORS is safe here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      checkMethod(tree);
    } else if (((AnnotationTree) tree).symbolType().is("org.springframework.web.bind.annotation.CrossOrigin")) {
      checkAnnotation((AnnotationTree) tree);
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

  private void checkAnnotation(AnnotationTree tree) {
    if (tree.arguments().stream().noneMatch(CORSCheck::setSpecificOrigins)) {
      reportTree(tree.annotationType());
    }
  }

  private static boolean setSpecificOrigins(Tree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
      ExpressionTree variable = assignment.variable();
      return variable.is(Tree.Kind.IDENTIFIER) &&
        ANNOTATION_ORIGINS_KEY_ALIAS.contains(((IdentifierTree) variable).name()) &&
        !isStar(assignment.expression());
    }
    return !isStar((LiteralTree) tree);
  }

  private void reportTree(MethodInvocationTree mit) {
    reportTree(ExpressionUtils.methodName(mit));
  }

  private void reportTree(Tree tree) {
    reportIssue(tree, MESSAGE);
  }

  private static boolean isStar(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.NEW_ARRAY)) {
      return ((NewArrayTree) expressionTree).initializers().stream().anyMatch(CORSCheck::isStar);
    } else {
      String value = ExpressionsHelper.getConstantValueAsString(expressionTree).value();
      return value != null && value.equals("*");
    }
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {
    List<MethodInvocationTree> addAllowedOrigin = new ArrayList<>();
    List<MethodInvocationTree> applyPermit = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (SET_ADD_HEADER_MATCHER.matches(mit)) {
        String headerName = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
        if (ACCESS_CONTROL_ALLOW_ORIGIN.equalsIgnoreCase(headerName) && isStar(mit.arguments().get(1))) {
          reportTree(mit);
        }
      } else if (APPLY_PERMIT_DEFAULT_VALUES.matches(mit)) {
        applyPermit.add(mit);
      } else if (ADD_ALLOWED_ORIGIN_MATCHER.matches(mit) && isStar(mit.arguments().get(0))) {
        addAllowedOrigin.add(mit);
      }
      super.visitMethodInvocation(mit);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // cut the visit
    }
  }
}
