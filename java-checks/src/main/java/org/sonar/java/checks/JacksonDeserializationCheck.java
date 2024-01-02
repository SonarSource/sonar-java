/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4544")
public class JacksonDeserializationCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers ENABLE_DEFAULT_TYPING = MethodMatchers.create()
    .ofTypes(
      "com.fasterxml.jackson.databind.ObjectMapper",
      "org.codehaus.jackson.map.ObjectMapper")
    .names("enableDefaultTyping")
    .addWithoutParametersMatcher()
    .build();

  private static final String MESSAGE = "Make sure using this Jackson deserialization configuration is safe here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION) && ENABLE_DEFAULT_TYPING.matches((MethodInvocationTree) tree)) {
      reportIssue(tree, MESSAGE);
    } else if (tree.is(Tree.Kind.ANNOTATION)) {
      AnnotationTree annotationTree = (AnnotationTree) tree;
      if (isJsonTypeInfo(annotationTree) && isAnnotationOnClassOrField(annotationTree)) {
        findUseArgument(annotationTree).ifPresent(useAnnotationArgument ->
            reportIssue(useAnnotationArgument, MESSAGE));
      }
    }
  }

  private static boolean isJsonTypeInfo(AnnotationTree annotationTree) {
    Type annotationType = annotationTree.annotationType().symbolType();
    return annotationType.is("com.fasterxml.jackson.annotation.JsonTypeInfo")
        || annotationType.is("org.codehaus.jackson.annotate.JsonTypeInfo");
  }

  private static boolean isAnnotationOnClassOrField(AnnotationTree annotationTree) {
    if (annotationTree.parent().is(Tree.Kind.MODIFIERS)) {
      Tree modifiers = annotationTree.parent();
      return modifiers.parent().is(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.VARIABLE);
    }
    return false;
  }

  private static Optional<ExpressionTree> findUseArgument(AnnotationTree annotationTree) {
    for (ExpressionTree tree : annotationTree.arguments()) {
      if (tree.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
        if ("use".equals(((IdentifierTree) assignment.variable()).name())
            && isJsonTypeIdEnumValue(assignment.expression())) {
          return Optional.of(assignment.expression());
        }
      }
    }
    return Optional.empty();
  }

  private static boolean isJsonTypeIdEnumValue(ExpressionTree tree) {
    if (!isJsonTypeId(tree)) {
      return false;
    }
    String valueName;
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      valueName = ((MemberSelectExpressionTree) tree).identifier().name();
    } else {
      valueName = ((IdentifierTree) tree).name();
    }
    return "CLASS".equals(valueName) || "MINIMAL_CLASS".equals(valueName);
  }

  private static boolean isJsonTypeId(ExpressionTree tree) {
    Type type = tree.symbolType();
    return type.is("com.fasterxml.jackson.annotation.JsonTypeInfo$Id")
        || type.is("org.codehaus.jackson.annotate.JsonTypeInfo$Id");
  }
}

