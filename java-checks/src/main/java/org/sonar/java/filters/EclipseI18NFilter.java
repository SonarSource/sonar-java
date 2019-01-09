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
package org.sonar.java.filters;

import com.google.common.collect.ImmutableSet;

import org.sonar.java.checks.ClassVariableVisibilityCheck;
import org.sonar.java.checks.PublicStaticFieldShouldBeFinalCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.ClassTree;

import java.util.Set;

public class EclipseI18NFilter extends BaseTreeVisitorIssueFilter {

  private static final Set<Class<? extends JavaCheck>> FILTERED_RULES = ImmutableSet.<Class<? extends JavaCheck>>of(
    PublicStaticFieldShouldBeFinalCheck.class,
    ClassVariableVisibilityCheck.class);

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return FILTERED_RULES;
  }

  @Override
  public void visitClass(ClassTree tree) {
    if (tree.symbol().type().isSubtypeOf("org.eclipse.osgi.util.NLS")) {
      excludeLines(tree, FILTERED_RULES);
    } else {
      acceptLines(tree, FILTERED_RULES);
    }
    super.visitClass(tree);
  }
}
