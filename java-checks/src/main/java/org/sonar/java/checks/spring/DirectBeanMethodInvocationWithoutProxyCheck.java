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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S6838")
public class DirectBeanMethodInvocationWithoutProxyCheck extends IssuableSubscriptionVisitor {
  private static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
  private static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";
  private static final String SCOPE_ANNOTATION = "org.springframework.context.annotation.Scope";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Optional<AnnotationTree> configurationAnnotation = getConfigurationAnnotation((ClassTree) tree);
    if (configurationAnnotation.isEmpty() || !hasProxyBeanMethodsDisabled(configurationAnnotation.get())) {
      return;
    }
    var visitor = new NonProxiedMethodInvocationVisitor((ClassTree) tree);
    tree.accept(visitor);
    visitor.locations.forEach(invocation -> reportIssue(invocation, "Replace this bean method invocation with a dependency injection."));
  }

  private static Optional<AnnotationTree> getConfigurationAnnotation(ClassTree tree) {
    return tree.modifiers().annotations().stream()
      .filter(annotationInstance -> annotationInstance.symbolType().is(CONFIGURATION_ANNOTATION))
      .findFirst();
  }


  private static boolean hasProxyBeanMethodsDisabled(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .anyMatch(DirectBeanMethodInvocationWithoutProxyCheck::setsProxyBeanMethodsToFalse);
  }

  private static boolean setsProxyBeanMethodsToFalse(AssignmentExpressionTree assignment) {
    return "proxyBeanMethods".equals(((IdentifierTree) assignment.variable()).name()) &&
      Boolean.FALSE.equals(ExpressionsHelper.getConstantValueAsBoolean(assignment.expression()).value());
  }

  private static class NonProxiedMethodInvocationVisitor extends BaseTreeVisitor {
    private final ClassTree parentClass;
    private final List<MethodInvocationTree> locations = new ArrayList<>();

    public NonProxiedMethodInvocationVisitor(ClassTree parentClass) {
      this.parentClass = parentClass;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      super.visitMethodInvocation(tree);
      MethodTree declaration = tree.methodSymbol().declaration();
      if (declaration == null || !isBeanMethod(declaration) || hasPrototypeScope(declaration)) {
        return;
      }
      Tree parent = declaration.parent();
      if (parent == parentClass) {
        locations.add(tree);
      }
    }

    private static boolean isBeanMethod(MethodTree tree) {
      return tree.modifiers().annotations().stream()
        .anyMatch(annotation -> annotation.symbolType().is(BEAN_ANNOTATION));
    }

    /*
     * A method with the prototype scope is meant to return a new instance on every call.
     */
    private static boolean hasPrototypeScope(MethodTree method) {
      List<SymbolMetadata.AnnotationValue> annotationValues = method.symbol().metadata().valuesForAnnotation(SCOPE_ANNOTATION);
      return annotationValues != null && annotationValues.stream()
        .filter(argument -> List.of("value", "scopeName").contains(argument.name()))
        .map(SymbolMetadata.AnnotationValue::value)
        .map(String.class::cast)
        .anyMatch("prototype"::equalsIgnoreCase);
    }
  }

}
