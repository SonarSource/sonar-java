/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1151")
public class SwitchCaseTooBigCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 5;

  @RuleProperty(
    description = "Maximum number of lines",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchTree switchTree = (SwitchTree) tree;
    var metricsComputer = ((MetricsScannerContext)context).getMetricsComputer();
    switchTree.cases().forEach(
      cgt -> {
        int lines = cgt.body().stream().mapToInt(metricsComputer::getLinesOfCode).sum();
        if (lines > max) {
          reportIssue(cgt.labels().get(cgt.labels().size() - 1),
            "Reduce this switch case number of lines from " + lines + " to at most " + max + ", for example by extracting code into methods.");
        }
      }
      );
  }
}
