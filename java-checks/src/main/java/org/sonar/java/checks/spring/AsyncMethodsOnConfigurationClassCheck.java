/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6817")
public class AsyncMethodsOnConfigurationClassCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    boolean isConfiguration = classTree.modifiers().annotations().stream()
      .anyMatch(annotation -> annotation.annotationType().symbolType().is(SpringUtils.CONFIGURATION_ANNOTATION));

    if (isConfiguration) {
      classTree.members().stream()
        .filter(member -> member.is(Tree.Kind.METHOD))
        .map(MethodTree.class::cast)
        .forEach(member -> member.modifiers().annotations().stream()
          .filter(annotation -> annotation.annotationType().symbolType().is(SpringUtils.ASYNC_ANNOTATION))
          .findFirst()
          .ifPresent(annotation -> QuickFixHelper.newIssue(context)
            .forRule(this)
            .onTree(annotation)
            .withMessage("Remove this \"@Async\" annotation from this method.")
            .withQuickFix(() -> JavaQuickFix.newQuickFix("Remove \"@Async\"")
              .addTextEdit(JavaTextEdit.removeTree(annotation))
              .build())
            .report()));
    }
  }

}
