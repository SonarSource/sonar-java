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
package org.sonar.java.checks.unused;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3984")
public class UnusedThrowableCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    if (newClassTree.symbolType().isSubtypeOf("java.lang.Throwable")) {
      Tree parent = newClassTree.parent();
      if (parent.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(newClassTree)
          .withMessage("Throw this exception or remove this useless statement.")
          .withQuickFixes(() -> Arrays.asList(
            JavaQuickFix.newQuickFix("Add \"throw\"")
              .addTextEdit(JavaTextEdit.insertBeforeTree(newClassTree, "throw "))
              .build(),
            JavaQuickFix.newQuickFix("Remove the statement")
              .addTextEdit(
                JavaTextEdit.removeTree(parent)
              ).build()))
          .report();
      }
    }
  }
}
