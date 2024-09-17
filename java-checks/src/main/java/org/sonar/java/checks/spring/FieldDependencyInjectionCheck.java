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
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Set;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6813")
public class FieldDependencyInjectionCheck extends IssuableSubscriptionVisitor {
  private static final Set<String> INJECTION_ANNOTATIONS = Set.of(
    "org.springframework.beans.factory.annotation.Autowired",
    "javax.inject.Inject",
    "jakarta.inject.Inject");

  private static final Set<String> SPRING_MANAGED_CLASS_ANNOTATIONS = Set.of(
    "org.springframework.stereotype.Component",
    "org.springframework.stereotype.Configuration",
    "org.springframework.stereotype.Controller",
    "org.springframework.stereotype.Repository",
    "org.springframework.stereotype.Service");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;

    if (isSpringManagedClass(classTree)) {
      classTree.members().forEach(this::reportOnInjectedField);
    }
  }

  private void reportOnInjectedField(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      var variableTree = (VariableTree) tree;

      variableTree.modifiers().annotations().stream()
        .filter(FieldDependencyInjectionCheck::isInjectionAnnotation)
        .findFirst()
        .ifPresent(annotationTree -> reportIssue(annotationTree, "Remove this field injection and use constructor injection instead."));
    }
  }

  private static boolean isSpringManagedClass(ClassTree classTree) {
    return classTree.modifiers().annotations().stream()
      .map(FieldDependencyInjectionCheck::getAnnotationName)
      .anyMatch(SPRING_MANAGED_CLASS_ANNOTATIONS::contains);
  }

  private static boolean isInjectionAnnotation(AnnotationTree annotationTree) {
    return INJECTION_ANNOTATIONS.contains(getAnnotationName(annotationTree));
  }

  private static String getAnnotationName(AnnotationTree annotationTree) {
    return annotationTree.annotationType().symbolType().fullyQualifiedName();
  }
}
