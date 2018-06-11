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
package org.sonar.java.checks.spring;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4507")
public class SpringSecurityDebugModeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree annotation = (AnnotationTree) tree;
    if (annotation.symbolType().is("org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")) {
      annotation.arguments().stream()
        .map(SpringSecurityDebugModeCheck::getDebugArgument)
        .filter(Objects::nonNull)
        .findFirst()
        .filter(assignment -> Boolean.TRUE.equals(ConstantUtils.resolveAsBooleanConstant(assignment.expression())))
        .ifPresent(assignment -> reportIssue(assignment, "Deactivate Spring Security's debug mode."));
    }
  }

  @CheckForNull
  private static AssignmentExpressionTree getDebugArgument(ExpressionTree expression) {
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
      if (assignment.variable().is(Tree.Kind.IDENTIFIER) &&
        "debug".equals(((IdentifierTree) assignment.variable()).name())) {
        return assignment;
      }
    }
    return null;
  }

}
