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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.TernaryValue;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1479")
public class SwitchWithTooManyCasesCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAXIMUM_CASES = 30;

  @RuleProperty(
    key = "maximum",
    description = "Maximum number of case",
    defaultValue = "" + DEFAULT_MAXIMUM_CASES)
  public int maximumCases = DEFAULT_MAXIMUM_CASES;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchTree switchTree = (SwitchTree) tree;
    if (isSwitchOverEnum(switchTree).maybeTrue()) {
      return;
    }

    List<CaseGroupTree> cases = switchTree.cases();
    int size = cases.size();
    if (size > maximumCases) {
      List<JavaFileScannerContext.Location> secondary = new ArrayList<>();
      for (CaseGroupTree element : cases) {
        secondary.add(new JavaFileScannerContext.Location("+1", element.labels().get(0)));
      }
      reportIssue(switchTree.switchKeyword(),
        String.format("Reduce the number of non-empty switch cases from %d to at most %d.", size, maximumCases),
        secondary, null);
    }
  }

  private static TernaryValue isSwitchOverEnum(SwitchTree switchStatementTree) {
    Symbol.TypeSymbol typeSymbol = switchStatementTree.expression().symbolType().symbol();
    return typeSymbol.isUnknown() ? TernaryValue.UNKNOWN : TernaryValue.of(typeSymbol.isEnum());
  }
}
