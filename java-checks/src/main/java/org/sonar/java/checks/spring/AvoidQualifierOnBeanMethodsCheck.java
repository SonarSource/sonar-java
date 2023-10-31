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
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6831")
public class AvoidQualifierOnBeanMethodsCheck extends IssuableSubscriptionVisitor {
  private static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";
  private static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";
  private static final String QUALIFIER_ANNOTATION = "org.springframework.beans.factory.annotation.Qualifier";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var classTree = (ClassTree) tree;

    if(hasAnnotation(classTree.modifiers(), CONFIGURATION_ANNOTATION)) {
      classTree.members()
        .stream()
        .filter(member -> member.is(Tree.Kind.METHOD))
        .map(MethodTree.class::cast)
        .filter(methodTree -> hasAnnotation(methodTree.modifiers(), BEAN_ANNOTATION))
        .filter(methodTree -> hasAnnotation(methodTree.modifiers(), QUALIFIER_ANNOTATION))
        .forEach(methodTree -> reportIssue(getQualifierAnnotation(methodTree), "Remove this redundant \"@Qualifier\" annotation"));
    }
  }

  private static boolean hasAnnotation(ModifiersTree modifiersTree, String annotation) {
    return modifiersTree.annotations()
      .stream()
      .anyMatch(annotationTree -> annotationTree.symbolType().is(annotation));
  }

  private static AnnotationTree getQualifierAnnotation(MethodTree methodTree) {
    return methodTree.modifiers()
      .annotations()
      .stream()
      .filter(annotationTree -> annotationTree.symbolType().is(QUALIFIER_ANNOTATION))
      .findFirst()
      .orElse(null);
  }
}
