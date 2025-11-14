/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6818")
public class AutowiredOnMultipleConstructorsCheck extends IssuableSubscriptionVisitor {

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
      .toList();

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
        .filter(a -> a.annotationType().symbolType().is(SpringUtils.AUTOWIRED_ANNOTATION))
        .findFirst();
      autowiredAnnotation.ifPresent(annotationTree -> reportIssue(annotationTree, "Remove this \"@Autowired\" annotation."));
    }

    return isAutowired;
  }

  private static boolean isAutowired(Symbol s) {
    if (s.metadata().isAnnotatedWith(SpringUtils.AUTOWIRED_ANNOTATION)) {
      List<SymbolMetadata.AnnotationValue> annotationValues = s.metadata().valuesForAnnotation(SpringUtils.AUTOWIRED_ANNOTATION);
      return annotationValues.isEmpty() || annotationValues.stream().anyMatch(a -> a.value().equals(true));
    }
    return false;
  }

}
