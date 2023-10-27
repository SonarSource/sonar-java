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
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6818")
public class AutowiredOnMultipleConstructorsCheck extends IssuableSubscriptionVisitor {

  private static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    List<MethodTree> constructors = classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.CONSTRUCTOR))
      .map(m -> (MethodTree) m)
      .collect(Collectors.toList());

    if (constructors.size() > 1) {
      boolean isAutowiredAlreadyFound = false;
      for (MethodTree constructor : constructors) {
        boolean isAutowired = checkConstructor(constructor, isAutowiredAlreadyFound);
        if (isAutowired) {
          isAutowiredAlreadyFound = true;
        }
      }
    }
  }

  private boolean checkConstructor(MethodTree methodTree, boolean isAutowiredAlreadyFound) {
    boolean isAutowired = isAutowired(methodTree.symbol());

    if (isAutowiredAlreadyFound && isAutowired) {
      Optional<AnnotationTree> autowiredAnnotation = methodTree.modifiers().annotations().stream()
        .filter(a -> a.annotationType().symbolType().is(AUTOWIRED_ANNOTATION))
        .findFirst();
      autowiredAnnotation.ifPresent(annotationTree -> reportIssue(annotationTree, "Remove this \"@Autowired\" annotation."));
    }

    return isAutowired;
  }

  private static boolean isAutowired(Symbol s) {
    if (s.metadata().isAnnotatedWith(AUTOWIRED_ANNOTATION)) {
      List<SymbolMetadata.AnnotationValue> annotationValues = s.metadata().valuesForAnnotation(AUTOWIRED_ANNOTATION);
      return annotationValues.isEmpty() || annotationValues.stream().anyMatch(a -> a.value().equals(true));
    }
    return false;
  }

}
