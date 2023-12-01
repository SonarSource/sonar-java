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
public class BeanMethodOfNonProxiedSingletonInvocationCheck extends IssuableSubscriptionVisitor {
  private static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";
  private static final String SCOPE_ANNOTATION = "org.springframework.context.annotation.Scope";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    boolean isTargetClass = getConfigurationAnnotation((ClassTree) tree)
      .map(BeanMethodOfNonProxiedSingletonInvocationCheck::hasProxyBeanMethodsDisabled)
      .orElse(Boolean.FALSE);
    if (!isTargetClass) {
      return;
    }
    var visitor = new NonProxiedMethodInvocationVisitor((ClassTree) tree);
    tree.accept(visitor);
    visitor.locations.forEach(invocation -> reportIssue(invocation, "Replace this bean method invocation " + "with a dependency injection."));
  }

  private static Optional<AnnotationTree> getConfigurationAnnotation(ClassTree tree) {
    SymbolMetadata metadata = tree.symbol().metadata();
    for (SymbolMetadata.AnnotationInstance instance : metadata.annotations()) {
      if (instance.symbol().type().is(CONFIGURATION_ANNOTATION)) {
        return Optional.ofNullable(metadata.findAnnotationTree(instance));
      }
    }
    return Optional.empty();
  }


  private static boolean hasProxyBeanMethodsDisabled(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .anyMatch(BeanMethodOfNonProxiedSingletonInvocationCheck::setsProxyBeanMethodsToFalse);
  }

  private static boolean setsProxyBeanMethodsToFalse(AssignmentExpressionTree assignment) {
    return "proxyBeanMethods".equals(((IdentifierTree) assignment.variable()).name()) &&
      Boolean.FALSE.equals(ExpressionsHelper.getConstantValueAsBoolean(assignment.expression()).value());
  }

  static class NonProxiedMethodInvocationVisitor extends BaseTreeVisitor {
    private final ClassTree parentClass;
    private final List<MethodInvocationTree> locations = new ArrayList<>();

    NonProxiedMethodInvocationVisitor(ClassTree parentClass) {
      this.parentClass = parentClass;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      super.visitMethodInvocation(tree);
      MethodTree declaration = tree.methodSymbol().declaration();
      if (declaration == null) {
        return;
      }
      // TODO check if the return type of the method returns a type with a prototype scope
      if (returnsAPrototypeBean(declaration)) {
        return;
      }
      Tree parent = declaration.parent();
      if (parent == parentClass) {
        locations.add(tree);
      }
    }

    private static boolean returnsAPrototypeBean(MethodTree method) {
      List<SymbolMetadata.AnnotationValue> annotationValues = method.symbol().metadata().valuesForAnnotation(SCOPE_ANNOTATION);
      if (annotationValues == null || annotationValues.isEmpty()) {
        return false;
      }
      return annotationValues.stream()
        .filter(argument -> List.of("value", "scopeName").contains(argument.name()))
        .map(SymbolMetadata.AnnotationValue::value)
        .anyMatch("Prototype"::equals);
    }
  }

}
