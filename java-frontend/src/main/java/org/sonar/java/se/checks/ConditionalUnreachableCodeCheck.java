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
package org.sonar.java.se.checks;

import org.sonar.check.Rule;
import org.sonar.java.se.AlwaysTrueOrFalseExpressionCollector;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Flow;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S2583")
public class ConditionalUnreachableCodeCheck extends SECheck {

  public static final String MESSAGE = "Change this condition so that it does not always evaluate to \"%s\"";

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    AlwaysTrueOrFalseExpressionCollector atof = context.alwaysTrueOrFalseExpressions();
    for (Tree condition : atof.alwaysFalse()) {
      reportBooleanExpression(context, atof, condition, false);
    }
    for (Tree condition : atof.alwaysTrue()) {
      reportBooleanExpression(context, atof, condition, true);
    }
  }

  private void reportBooleanExpression(CheckerContext context, AlwaysTrueOrFalseExpressionCollector atof, Tree condition, boolean isTrue) {
    if (AlwaysTrueOrFalseExpressionCollector.hasUnreachableCode(condition, isTrue)) {
      Set<Flow> flows = atof.flowForExpression(condition).stream()
        .map(flow -> AlwaysTrueOrFalseExpressionCollector.addIssueLocation(flow, condition, isTrue))
        .collect(Collectors.toSet());
      context.reportIssue(condition, this, String.format(MESSAGE, isTrue), flows);
    }
  }

}
