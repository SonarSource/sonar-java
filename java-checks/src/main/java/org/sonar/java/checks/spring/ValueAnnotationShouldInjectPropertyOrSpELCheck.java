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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6804")
public class ValueAnnotationShouldInjectPropertyOrSpELCheck extends IssuableSubscriptionVisitor {

  private static final String SPRING_VALUE = "org.springframework.beans.factory.annotation.Value";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree cls = (ClassTree) tree;

    List<AnnotationTree> fieldsAnnotations = cls.members()
      .stream()
      .filter(m -> m.is(Tree.Kind.VARIABLE))
      .flatMap(field -> ((VariableTree) field).modifiers().annotations().stream())
      .collect(Collectors.toList());

    List<AnnotationTree> interfaceAnnotations = cls.is(Tree.Kind.ANNOTATION_TYPE) ? cls.modifiers().annotations() : List.of();

    Stream.concat(fieldsAnnotations.stream(), interfaceAnnotations.stream())
      .filter(ValueAnnotationShouldInjectPropertyOrSpELCheck::isSimpleSpringValue)
      .forEach(ann -> reportIssue(
        ann,
        "Either replace the \"@Value\" annotation with a standard field initialization," +
          " use \"${propertyName}\" to inject a property " +
          "or use \"#{expression}\" to evaluate a SpEL expression."));
  }

  private static boolean isSimpleSpringValue(AnnotationTree annotation) {
    if (annotation.symbolType().is(SPRING_VALUE)) {
      String value = extractArgumentValue(annotation.arguments().get(0));
      return value != null && !(isPropertyName(value) || isSpEL(value));
    }
    return false;
  }

  @CheckForNull
  private static String extractArgumentValue(ExpressionTree annotationArgument) {
    if (annotationArgument.is(Tree.Kind.ASSIGNMENT)) {
      ExpressionTree expression = ((AssignmentExpressionTree) annotationArgument).expression();
      return ExpressionsHelper.getConstantValueAsString(expression).value();
    }
    return ExpressionsHelper.getConstantValueAsString(annotationArgument).value();
  }

  private static boolean isPropertyName(String value) {
    return value.startsWith("${") && value.endsWith("}");
  }

  private static boolean isSpEL(String value) {
    return value.startsWith("#{") && value.endsWith("}");
  }

}
