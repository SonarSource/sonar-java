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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.SpringUtils.SCOPE_ANNOTATION;
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
      && clazzMeta.isAnnotatedWith(SCOPE_ANNOTATION)
      && !isScopeSingleton(clazzMeta)) {
      checkScopeAnnotation(clazzTree);
    }
  }

  private static boolean isSpringComponent(SymbolMetadata clazzMeta) {
    return clazzMeta.isAnnotatedWith(SpringUtils.CONTROLLER_ANNOTATION)
      || clazzMeta.isAnnotatedWith(SpringUtils.SERVICE_ANNOTATION)
      || clazzMeta.isAnnotatedWith(SpringUtils.REPOSITORY_ANNOTATION);
  }

  private void checkScopeAnnotation(ClassTree tree) {
    tree.modifiers().annotations().stream()
      .filter(a -> a.annotationType().symbolType().fullyQualifiedName().equals(SCOPE_ANNOTATION))
      .forEach(a -> reportIssue(a, "Remove this \"@Scope\" annotation."));
  }

}
