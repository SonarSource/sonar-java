/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6837")
public class SuperfluousResponseBodyAnnotationCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    var ct = (ClassTree) tree;
    if (!ct.symbol().metadata().isAnnotatedWith(SpringUtils.REST_CONTROLLER_ANNOTATION)) {
      return;
    }

    ct.members().stream().filter(member -> member.is(Tree.Kind.METHOD)).forEach(member -> {
      var mt = (MethodTree) member;
      mt.modifiers().annotations().stream()
        .filter(annotationTree -> annotationTree.symbolType().is("org.springframework.web.bind.annotation.ResponseBody"))
        .findFirst()
        .ifPresent(annotationTree -> reportIssue(annotationTree, "Remove this superfluous \"@ResponseBody\" annotation."));
    });
  }
}
