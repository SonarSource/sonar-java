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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.SpringUtils.SPRING_SCOPE_ANNOTATION;
import static org.sonar.java.checks.helpers.SpringUtils.isScopeSingleton;

@Rule(key = "S3750")
public class SpringComponentWithWrongScopeCheck extends IssuableSubscriptionVisitor {


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree clazzTree = (ClassTree) tree;
    SymbolMetadata clazzMeta = clazzTree.symbol().metadata();

    if (isSpringComponent(clazzMeta)
      && clazzMeta.isAnnotatedWith(SPRING_SCOPE_ANNOTATION)
      && !isScopeSingleton(clazzMeta)) {
      checkScopeAnnotation(clazzTree);
    }
  }

  private static boolean isSpringComponent(SymbolMetadata clazzMeta) {
    return clazzMeta.isAnnotatedWith("org.springframework.stereotype.Controller")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Service")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Repository");
  }

  private void checkScopeAnnotation(ClassTree tree) {
    tree.modifiers().annotations().stream()
      .filter(a -> a.annotationType().symbolType().fullyQualifiedName().equals(SPRING_SCOPE_ANNOTATION))
      .forEach(a -> reportIssue(a, "Remove this \"@Scope\" annotation."));
  }

}
