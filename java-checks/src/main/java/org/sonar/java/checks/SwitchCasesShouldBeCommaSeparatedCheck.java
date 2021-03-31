/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6208")
public class SwitchCasesShouldBeCommaSeparatedCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "Merge the previous cases into this one using comma-separated label.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.SWITCH_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchExpressionTree switchExpression = (SwitchExpressionTree) tree;
    for (CaseGroupTree aCase : switchExpression.cases()) {
      List<CaseLabelTree> labels = aCase.labels();
      int size = labels.size();
      if (size == 1) {
        continue;
      }
      CaseLabelTree lastLabel = labels.get(size - 1);
      List<JavaFileScannerContext.Location> secondaries = labels.stream()
        .limit(size - 1L)
        .map(label -> new JavaFileScannerContext.Location("", label))
        .collect(Collectors.toList());
      reportIssue(lastLabel, MESSAGE, secondaries, null);
    }
    super.visitNode(tree);
  }
}
