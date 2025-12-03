/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
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
          .withSecondaries(caseLabels.stream().map(label -> new JavaFileScannerContext.Location("", label)).toList())
          .report();
      }

    }
    super.visitNode(tree);
  }

  public static boolean usesColons(SwitchTree tree) {
    return !tree.cases().isEmpty() &&
      ":".equals(tree.cases().get(0).labels().get(0).colonOrArrowToken().text());
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return 14 <= version.asInt();
  }
}
