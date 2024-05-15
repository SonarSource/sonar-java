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
package org.sonar.java.se.filters;

import java.util.Set;
import org.sonar.java.se.checks.XxeProcessingCheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class LombokFilter extends BaseTreeVisitorSEIssueFilter {

  private static final String LOMBOK_VAL = "lombok.val";

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return Set.of(XxeProcessingCheck.class);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    excludeLinesIfTrue(tree.symbol().type().is(LOMBOK_VAL) && tree.initializer() != null, tree.initializer(), XxeProcessingCheck.class);
    super.visitVariable(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    excludeLinesIfTrue(tree.variable().symbolType().is(LOMBOK_VAL), tree.expression(), XxeProcessingCheck.class);
    super.visitAssignmentExpression(tree);
  }

}
