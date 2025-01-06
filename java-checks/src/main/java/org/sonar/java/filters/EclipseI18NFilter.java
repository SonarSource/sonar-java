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
package org.sonar.java.filters;

import java.util.Set;
import org.sonar.java.checks.ClassVariableVisibilityCheck;
import org.sonar.java.checks.PublicStaticFieldShouldBeFinalCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.ClassTree;

public class EclipseI18NFilter extends BaseTreeVisitorIssueFilter {

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return Set.of(
      /* S1104 */ ClassVariableVisibilityCheck.class,
      /* S1444 */ PublicStaticFieldShouldBeFinalCheck.class);
  }

  @Override
  public void visitClass(ClassTree tree) {
    excludeLinesIfTrue(tree.symbol().type().isSubtypeOf("org.eclipse.osgi.util.NLS"), tree, PublicStaticFieldShouldBeFinalCheck.class, ClassVariableVisibilityCheck.class);
    super.visitClass(tree);
  }
}
