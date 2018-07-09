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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4529")
public class SpringEndpointsCheck extends IssuableSubscriptionVisitor {

  private static final String REQUEST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree annotationTree = (AnnotationTree) tree;
    if (isSpringWebHandler(annotationTree)) {
      findParentMethod(annotationTree).ifPresent(annotatedMethod ->
          reportIssue(annotatedMethod.simpleName(), "Review this Spring request handler"));
    }
  }

  private static boolean isSpringWebHandler(AnnotationTree annotationTree) {
    Type annotationType = annotationTree.annotationType().symbolType();
    return annotationType.is(REQUEST_MAPPING_ANNOTATION)
        || annotationType.symbol().metadata().isAnnotatedWith(REQUEST_MAPPING_ANNOTATION);
  }

  private static Optional<MethodTree> findParentMethod(AnnotationTree annotationTree) {
    Tree parent = annotationTree.parent();
    while (parent != null && !parent.is(Tree.Kind.METHOD)) {
      parent = parent.parent();
    }
    return Optional.ofNullable((MethodTree) parent);
  }
}
