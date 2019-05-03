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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S2093")
public class TryWithResourcesCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private final Deque<TryStatementTree> withinTry = new LinkedList<>();
  private final Deque<List<Tree>> toReport = new LinkedList<>();

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    withinTry.clear();
    toReport.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.TRY_STATEMENT, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.TRY_STATEMENT)) {
      withinTry.push((TryStatementTree) tree);
      toReport.push(new ArrayList<>());
    } else if (withinStandardTryWithFinally() && ((NewClassTree) tree).symbolType().isSubtypeOf("java.lang.AutoCloseable")) {
      toReport.peek().add(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.TRY_STATEMENT)) {
      TryStatementTree tryStatementTree = withinTry.pop();
      List<Tree> secondaryTrees = toReport.pop();
      if (!secondaryTrees.isEmpty()) {
        List<JavaFileScannerContext.Location> secondary = new ArrayList<>();
        for (Tree autoCloseable : secondaryTrees) {
          secondary.add(new JavaFileScannerContext.Location("AutoCloseable resource", autoCloseable));
        }
        reportIssue(tryStatementTree.tryKeyword(), "Change this \"try\" to a try-with-resources." + context.getJavaVersion().java7CompatibilityMessage(), secondary, null);
      }
    }
  }

  private boolean withinStandardTryWithFinally() {
    return !withinTry.isEmpty() && withinTry.peek().resourceList().isEmpty() && withinTry.peek().finallyBlock() != null;
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isNotSet() || version.asInt() >= 7;
  }
}
