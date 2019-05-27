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
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4784")
public class RegexHotspotCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatcherCollection REGEX_HOTSPOTS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("matches").addParameter(JAVA_LANG_STRING),
    MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("replaceAll").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("replaceFirst").withAnyParameters(),
    MethodMatcher.create().typeDefinition("java.util.regex.Pattern").name("compile").withAnyParameters(),
    MethodMatcher.create().typeDefinition("java.util.regex.Pattern").name("matches").withAnyParameters()
  );
  private static final String MESSAGE = "Make sure that using a regular expression is safe here.";
  private static final List<String> HOTSPOT_ANNOTATION_TYPES = Arrays.asList(
    "javax.validation.constraints.Pattern",
    "javax.validation.constraints.Email",
    "org.hibernate.validator.constraints.URL"
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      if (REGEX_HOTSPOTS.anyMatch((MethodInvocationTree) tree)) {
        Arguments args = ((MethodInvocationTree) tree).arguments();
        if (!args.isEmpty() && isSuspiciousRegex(args.get(0))) {
          reportIssue(args.get(0), MESSAGE);
        }
      }
    } else {
      // annotations
      AnnotationTree annotationTree = (AnnotationTree) tree;
      if (HOTSPOT_ANNOTATION_TYPES.stream().anyMatch(t -> annotationTree.annotationType().symbolType().is(t))) {
        annotationTree.arguments().stream().filter(RegexHotspotCheck::isRegexpParameter)
          .findFirst().ifPresent(e -> reportIssue(e, MESSAGE));
      }
    }
  }

  private static boolean isRegexpParameter(ExpressionTree expr) {
    if(expr.is(Tree.Kind.ASSIGNMENT) && ((AssignmentExpressionTree) expr).variable().is(Tree.Kind.IDENTIFIER)) {
      AssignmentExpressionTree aet = (AssignmentExpressionTree) expr;
      IdentifierTree variable = (IdentifierTree) aet.variable();
      return variable.name().equals("regexp") && isSuspiciousRegex(aet.expression());
    }
    return false;
  }

  /**
   * This rule flags any execution of a hardcoded regular expression which has at least 3 characters and at least
   * two instances of any of the following characters: "*+{" (Example: (a+)*)
   * */
  private static boolean isSuspiciousRegex(ExpressionTree exp) {
    String regexp = ExpressionsHelper.getConstantValueAsString(exp).value();
    if (regexp != null && regexp.length() > 2) {
      int nOfSuspiciousChars = regexp.length() - regexp.replaceAll("[*+{]", "").length();
      return nOfSuspiciousChars > 1;
    }
    return false;
  }
}
