/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3750")
public class SpringComponentWithWrongScopeCheck extends IssuableSubscriptionVisitor {

  private static final String SCOPE_ANNOTATION_FQN = "org.springframework.context.annotation.Scope";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree clazzTree = (ClassTree) tree;
    SymbolMetadata clazzMeta = clazzTree.symbol().metadata();

    if (isSpringComponent(clazzMeta)
      && clazzMeta.isAnnotatedWith(SCOPE_ANNOTATION_FQN)
      && !isScopeSingleton(clazzMeta)) {
      checkScopeAnnotation(clazzTree);
    }
  }

  private static boolean isSpringComponent(SymbolMetadata clazzMeta) {
    return clazzMeta.isAnnotatedWith("org.springframework.stereotype.Controller")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Service")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Repository");
  }

  private static boolean isScopeSingleton(SymbolMetadata clazzMeta) {
    List<AnnotationValue> values = clazzMeta.valuesForAnnotation(SCOPE_ANNOTATION_FQN);
    for (AnnotationValue annotationValue : values) {
      if (("value".equals(annotationValue.name()) || "scopeName".equals(annotationValue.name()))
        && annotationValue.value() instanceof LiteralTree
        && !"\"singleton\"".equals(((LiteralTree) annotationValue.value()).value())) {
        return false;
      }
    }
    return true;
  }

  private void checkScopeAnnotation(ClassTree tree) {
    tree.modifiers().annotations().stream()
      .filter(a -> a.annotationType().symbolType().fullyQualifiedName().equals(SCOPE_ANNOTATION_FQN))
      .forEach(a -> reportIssue(a, "Remove this \"@Scope\" annotation."));
  }

}
