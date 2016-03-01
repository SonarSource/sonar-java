/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

public abstract class IssuableSubscriptionVisitor extends SubscriptionVisitor {

  /**
   * @deprecated use reportIssue instead to benefit from precise issue location.
   */
  @Deprecated
  public void addIssue(Tree tree, String message) {
    context.addIssue(tree, this, message);
  }

  /**
   * @deprecated use reportIssue instead to benefit from precise issue location.
   */
  @Deprecated
  public void addIssue(Tree tree, String message, double effortToFix) {
    context.addIssue(tree, this, message, effortToFix);
  }

  public void addIssue(int line, String message) {
    context.addIssue(line, this, message);
  }

  public void addIssueOnFile(String message) {
    context.addIssueOnFile(this, message);
  }

  public void reportIssue(Tree tree, String message) {
    context.reportIssue(this, tree, message);
  }

  public void reportIssue(Tree tree, String message, List<JavaFileScannerContext.Location> flow, @Nullable Integer cost) {
    context.reportIssue(this, tree, message, flow, cost);
  }

  public void reportIssue(Tree startTree, Tree endTree, String message) {
    context.reportIssue(this, startTree, endTree, message);
  }
}
