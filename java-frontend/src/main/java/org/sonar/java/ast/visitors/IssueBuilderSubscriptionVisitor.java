/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.ast.visitors;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class IssueBuilderSubscriptionVisitor extends IssuableSubscriptionVisitor {

  private static final String ERROR_MESSAGE = "IssueBuilderSubsciptionVisitor should only use newIssue().";

  public FluentReporting.JavaIssueBuilder newIssue() {
    return ((FluentReporting) context).newIssue().forRule(this);
  }

  @Override
  public final void reportIssue(Tree startTree, Tree endTree, String message) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public final void reportIssue(Tree tree, String message) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public final void reportIssue(Tree tree, String message, List<Location> flow, @Nullable Integer cost) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  public final void addIssue(int line, String message) {
    throw new UnsupportedOperationException(ERROR_MESSAGE);
  }

  @Override
  protected final void scanTree(Tree tree) {
    throw new UnsupportedOperationException("IssueBuilderSubsciptionVisitor should not drive visit of AST.");
  }
}
