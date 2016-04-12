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
package org.sonar.java.filters;

import com.google.common.collect.ImmutableSet;

import org.sonar.plugins.java.api.tree.ClassTree;

import java.util.Set;

public class EclipseI18NFilter extends BaseTreeVisitorIssueFilter {

  @Override
  public Set<String> targetedRules() {
    return ImmutableSet.of(
      // "public static" fields should be constant
      "S1444",
      // Class variable fields should not have public accessibility (with legacy key)
      "S1104", "ClassVariableVisibilityCheck");
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (tree.symbol().type().isSubtypeOf("org.eclipse.osgi.util.NLS")) {
      ignoreIssuesInTree(tree);
    }
    super.visitClass(tree);
  }
}
