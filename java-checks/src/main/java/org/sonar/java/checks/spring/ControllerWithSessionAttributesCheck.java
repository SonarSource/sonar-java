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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3753")
public class ControllerWithSessionAttributesCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ClassTree classTree = (ClassTree) tree;
    SymbolMetadata classMetadata = classTree.symbol().metadata();
    Optional<AnnotationTree> sessionAttributesAnnotation = classTree.modifiers().annotations()
      .stream()
      .filter(a -> a.annotationType().symbolType().fullyQualifiedName().equals("org.springframework.web.bind.annotation.SessionAttributes"))
      .findFirst();
    if (classMetadata.isAnnotatedWith("org.springframework.stereotype.Controller")
        && sessionAttributesAnnotation.isPresent()
        && classTree.members().stream().noneMatch(ControllerWithSessionAttributesCheck::methodCompletesSessionStatus)) {
      reportIssue(sessionAttributesAnnotation.get().annotationType(),
          "Add a call to \"setComplete()\" on the SessionStatus object in a \"@RequestMapping\" method that handles \"POST\".");
    }
  }

  private static boolean methodCompletesSessionStatus(Tree tree) {
    if (!tree.is(Tree.Kind.METHOD)) {
      return false;
    }

    MethodTree methodTree = (MethodTree) tree;
    List<AnnotationTree> annotationTrees = methodTree.modifiers().annotations();
    if (annotationTrees.stream().anyMatch(ControllerWithSessionAttributesCheck::isPostRequest)
        && methodTree.parameters().stream().anyMatch(p -> p.type().symbolType().fullyQualifiedName().equals("org.springframework.web.bind.support.SessionStatus"))) {
      MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor();
      methodTree.block().accept(methodInvocationVisitor);
      return methodInvocationVisitor.found;
    }
    return false;
  }

  private static boolean isPostRequest(AnnotationTree annotation) {
    if (annotation.symbolType().is("org.springframework.web.bind.annotation.RequestMapping")) {
      List<ExpressionTree> methodValues = annotation.arguments().stream()
          .filter(argument -> "method".equals(attributeName(argument)))
          .flatMap(ControllerWithSessionAttributesCheck::extractValues)
          .collect(Collectors.toList());
      if (methodValues.size() == 1) {
        return getRequestMethodEnumEntry(methodValues.get(0)).equals("POST");
      }
      return false;
    } else {
      return annotation.symbolType().is("org.springframework.web.bind.annotation.PostMapping");
    }
  }

  // FIXME copy pasted from SpringComposedRequestMappingCheck
  private static String getRequestMethodEnumEntry(ExpressionTree requestMethod) {
    ExpressionTree expression = requestMethod;
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) requestMethod).identifier();
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (symbol.type().is("org.springframework.web.bind.annotation.RequestMethod")) {
        return symbol.name();
      }
    }
    return "";
  }

  // FIXME copy pasted from SpringComposedRequestMappingCheck
  private static String attributeName(ExpressionTree expression) {
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
      // assignment.variable() in annotation is always a Tree.Kind.IDENTIFIER
      return ((IdentifierTree) assignment.variable()).name();
    }
    return "";
  }

  // FIXME copy pasted from SpringComposedRequestMappingCheck
  private static Stream<ExpressionTree> extractValues(ExpressionTree argument) {
    ExpressionTree expression = argument;
    if (expression.is(Tree.Kind.ASSIGNMENT)) {
      expression = ((AssignmentExpressionTree) expression).expression();
    }
    if (expression.is(Tree.Kind.NEW_ARRAY)) {
      return ((NewArrayTree) expression).initializers().stream()
          .flatMap(ControllerWithSessionAttributesCheck::extractValues);
    }
    return Stream.of(expression);
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {
    boolean found;
    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.symbol().toString().equals("SessionStatus#setComplete()")) {
        found = true;
      }
    }
  }
}
