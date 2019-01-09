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
package org.sonar.plugins.java.api;

import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Subscription visitor providing an API to report issues.
 */
public abstract class IssuableSubscriptionVisitor extends SubscriptionVisitor {

  @Override
  protected void scanTree(Tree tree) {
    throw new UnsupportedOperationException("IssuableSubscriptionVisitor should not drive visit of AST.");
  }

  /**
   * Report an issue on a specific line.
   * @param line line on which to report the issue
   * @param message Message to display to the user
   */
  public void addIssue(int line, String message) {
    context.addIssue(line, this, message);
  }

  /**
   * Report an issue at file level.
   * @param message Message to display to the user
   */
  public void addIssueOnFile(String message) {
    context.addIssueOnFile(this, message);
  }

  /**
   * Report an issue.
   * @param tree syntax node on which to raise the issue.
   * @param message Message to display to the user.
   */
  public void reportIssue(Tree tree, String message) {
    context.reportIssue(this, tree, message);
  }

  /**
   * Report an issue.
   * @param tree syntax node on which to raise the issue.
   * @param message Message to display to the user.
   * @param flow List of {@link JavaFileScannerContext.Location} to display secondary locations describing the flow leading to the issue.
   *   Empty list if the issue does not requires secondary location.
   * @param cost computed remediation cost if applicable, null if not.
   */
  public void reportIssue(Tree tree, String message, List<JavaFileScannerContext.Location> flow, @Nullable Integer cost) {
    context.reportIssue(this, tree, message, flow, cost);
  }

  /**
   * Report an issue.
   * @param startTree syntax node on which to start the highlighting of the issue.
   * @param endTree syntax node on which to end the highlighting of the issue.
   * @param message Message to display to the user.
   */
  public void reportIssue(Tree startTree, Tree endTree, String message) {
    context.reportIssue(this, startTree, endTree, message);
  }
}
