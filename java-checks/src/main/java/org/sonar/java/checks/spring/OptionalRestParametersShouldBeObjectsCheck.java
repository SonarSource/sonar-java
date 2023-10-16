/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6814")
public class OptionalRestParametersShouldBeObjectsCheck extends IssuableSubscriptionVisitor {

  private static final String PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    method.parameters().stream()
      .filter(OptionalRestParametersShouldBeObjectsCheck::isOptionalPrimitive)
      .forEach(parameter -> reportIssue(parameter, "Convert this optional parameter to an Object type."));
  }

  private static boolean isOptionalPrimitive(VariableTree parameter) {
    return parameter.type().symbolType().isPrimitive() &&
      parameter.modifiers().annotations().stream()
        .anyMatch(OptionalRestParametersShouldBeObjectsCheck::isPathVariableOptional);
  }

  private static boolean isPathVariableOptional(AnnotationTree annotation) {
    return annotation.annotationType().symbolType().is(PATH_VARIABLE_ANNOTATION) &&
      annotation.arguments().stream().anyMatch(expressionTree -> {
        // Because all parameters of the PathVariable annotation are assignments, we can cast safely here.
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) expressionTree;
        IdentifierTree variable = (IdentifierTree) assignment.variable();
        Boolean constant = assignment.expression().asConstant(Boolean.class).orElse(Boolean.TRUE);
        return "required".equals(variable.name()) && Boolean.FALSE.equals(constant);
      });
  }
}
