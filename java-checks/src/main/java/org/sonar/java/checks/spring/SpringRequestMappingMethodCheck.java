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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3752")
public class SpringRequestMappingMethodCheck extends IssuableSubscriptionVisitor {

  private static final String REQUEST_MAPPING_CLASS = "org.springframework.web.bind.annotation.RequestMapping";

  private static final String REQUEST_METHOD = "method";
  public static final String MESSAGE = "Make sure allowing safe and unsafe HTTP methods is safe here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    findRequestMappingAnnotation(classTree.modifiers())
      .flatMap(SpringRequestMappingMethodCheck::findRequestMethods)
      .filter(SpringRequestMappingMethodCheck::mixSafeAndUnsafeMethods)
      .ifPresent(methods -> reportIssue(methods, MESSAGE));

    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .forEach(member -> checkMethod((MethodTree) member, classTree.symbol()));
  }

  private void checkMethod(MethodTree method, Symbol.TypeSymbol classSymbol) {
    Optional<AnnotationTree> requestMappingAnnotation = findRequestMappingAnnotation(method.modifiers());
    Optional<ExpressionTree> requestMethods = requestMappingAnnotation
      .flatMap(SpringRequestMappingMethodCheck::findRequestMethods);

    if (requestMethods.isPresent()) {
      requestMethods
        .filter(SpringRequestMappingMethodCheck::mixSafeAndUnsafeMethods)
        .ifPresent(methods -> reportIssue(methods, MESSAGE));
    } else if (requestMappingAnnotation.isPresent() && !inheritRequestMethod(classSymbol)) {
      reportIssue(requestMappingAnnotation.get().annotationType(), MESSAGE);
    }
  }

  private static Optional<AnnotationTree> findRequestMappingAnnotation(ModifiersTree modifiers) {
    return modifiers.annotations().stream()
      .filter(annotation -> annotation.symbolType().is(REQUEST_MAPPING_CLASS))
      .findFirst();
  }

  private static Optional<ExpressionTree> findRequestMethods(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .filter(assignment -> REQUEST_METHOD.equals(((IdentifierTree) assignment.variable()).name()))
      .map(AssignmentExpressionTree::expression)
      .findFirst();
  }

  private static boolean inheritRequestMethod(Symbol.TypeSymbol symbol) {
    List<SymbolMetadata.AnnotationValue> annotationValues = symbol.metadata().valuesForAnnotation(REQUEST_MAPPING_CLASS);
    if (annotationValues != null && annotationValues.stream().anyMatch(value -> REQUEST_METHOD.equals(value.name()))) {
      return true;
    }
    Type superClass = symbol.superClass();
    if (superClass != null && inheritRequestMethod(superClass.symbol())) {
      return true;
    }
    for (Type type : symbol.interfaces()) {
      if (inheritRequestMethod(type.symbol())) {
        return true;
      }
    }
    return false;
  }

  private static boolean mixSafeAndUnsafeMethods(ExpressionTree requestMethodsAssignment) {
    HttpMethodVisitor visitor = new HttpMethodVisitor();
    requestMethodsAssignment.accept(visitor);
    return visitor.hasSafeMethods && visitor.hasUnsafeMethods;
  }

  private static class HttpMethodVisitor extends BaseTreeVisitor {
    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE"));
    private static final Set<String> UNSAFE_METHODS = new HashSet<>(Arrays.asList("DELETE", "PATCH", "POST", "PUT"));

    private boolean hasSafeMethods = false;
    private boolean hasUnsafeMethods = false;

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      hasSafeMethods |= SAFE_METHODS.contains(tree.name());
      hasUnsafeMethods |= UNSAFE_METHODS.contains(tree.name());
    }
  }

}
