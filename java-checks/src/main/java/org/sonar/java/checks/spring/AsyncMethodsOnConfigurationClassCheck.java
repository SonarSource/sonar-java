/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.java.checks.helpers.QuickFixHelper;
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
      .anyMatch(annotation -> annotation.annotationType().symbolType().is("org.springframework.context.annotation.Configuration"));

    if (isConfiguration) {
      classTree.members().stream()
        .filter(member -> member.is(Tree.Kind.METHOD))
        .map(MethodTree.class::cast)
        .forEach(member -> member.modifiers().annotations().stream()
          .filter(annotation -> annotation.annotationType().symbolType().is("org.springframework.scheduling.annotation.Async"))
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
