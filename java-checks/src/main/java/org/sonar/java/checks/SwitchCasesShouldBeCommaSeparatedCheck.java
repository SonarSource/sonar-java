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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6208")
public class SwitchCasesShouldBeCommaSeparatedCheck extends SubscriptionVisitor implements JavaVersionAwareVisitor {
  private static final String MESSAGE = "Merge the previous cases into this one using comma-separated label.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.SWITCH_EXPRESSION,
      Tree.Kind.SWITCH_STATEMENT
    );
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchTree switchExpression = (SwitchTree) tree;
    if (!usesColons(switchExpression)) {
      return;
    }
    for (CaseGroupTree aCase : switchExpression.cases()) {
      List<CaseLabelTree> labels = aCase.labels();
      int size = labels.size();
      if (size == 1) {
        continue;
      }

      Deque<CaseLabelTree> caseLabels = labels.stream()
        .filter(label -> "case".equals(label.caseOrDefaultKeyword().text()))
        .collect(Collectors.toCollection(ArrayDeque::new));

      if (caseLabels.size() > 1) {
        CaseLabelTree lastLabel = caseLabels.removeLast();
        ((DefaultJavaFileScannerContext) context).newIssue()
          .forRule(this)
          .onTree(lastLabel)
          .withMessage(MESSAGE)
          .withSecondaries(caseLabels.stream().map(label -> new JavaFileScannerContext.Location("", label)).collect(Collectors.toList()))
          .build();
      }

    }
    super.visitNode(tree);
  }

  public static boolean usesColons(SwitchTree tree) {
    return !tree.cases().isEmpty() &&
      tree.cases().get(0).labels().get(0).colonOrArrowToken().text().equals(":");
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return 14 <= version.asInt();
  }
}
