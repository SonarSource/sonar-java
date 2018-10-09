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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4529")
public class SpringEndpointsCheck extends IssuableSubscriptionVisitor {

  private static final String REQUEST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping";
  private static final String MESSAGE = "Make sure that exposing this HTTP endpoint is safe here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    List<AnnotationTree> annotations = methodTree.modifiers().annotations();
    annotations.stream().filter(SpringEndpointsCheck::isSpringWebHandler).forEach(annotationTree ->
        reportIssue(methodTree.simpleName(), MESSAGE));
  }

  private static boolean isSpringWebHandler(AnnotationTree annotationTree) {
    Type annotationType = annotationTree.annotationType().symbolType();
    return annotationType.is(REQUEST_MAPPING_ANNOTATION)
        || annotationType.symbol().metadata().isAnnotatedWith(REQUEST_MAPPING_ANNOTATION);
  }
}
