/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S2583")
public class ConditionalUnreachableCodeCheck extends SECheck {

  public static final String MESSAGE = "Change this condition so that it does not always evaluate to \"%s\"";

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    CheckerContext.AlwaysTrueOrFalseExpressions atof = context.alwaysTrueOrFalseExpressions();
    for (Tree condition : atof.alwaysFalse()) {
      Set<List<JavaFileScannerContext.Location>> flows = atof.flowForExpression(condition).stream()
        .map(flow -> addIssueLocation(flow, condition, false)).collect(Collectors.toSet());
      context.reportIssue(condition, this, String.format(MESSAGE, false), flows);
    }
    for (Tree condition : atof.alwaysTrue()) {
      Set<List<JavaFileScannerContext.Location>> flows = atof.flowForExpression(condition).stream()
        .map(flow -> addIssueLocation(flow, condition, true)).collect(Collectors.toSet());
      context.reportIssue(condition, this, String.format(MESSAGE, true), flows);
    }
  }

  private static List<JavaFileScannerContext.Location> addIssueLocation(List<JavaFileScannerContext.Location> flow, Tree issueTree, boolean conditionIsAlwaysTrue) {
    return ImmutableList.<JavaFileScannerContext.Location>builder()
      .add(new JavaFileScannerContext.Location("Condition is always " + conditionIsAlwaysTrue + ".", issueTree))
      .addAll(flow)
      .build();
  }
}
