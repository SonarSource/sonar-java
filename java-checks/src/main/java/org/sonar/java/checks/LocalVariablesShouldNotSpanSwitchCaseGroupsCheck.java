/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9364")
public class LocalVariablesShouldNotSpanSwitchCaseGroupsCheck extends IssuableSubscriptionVisitor {

  static final String PRIMARY_MESSAGE = "Declare a separate variable in each case group; sharing this local variable across groups obscures its scope.";
  static final String SECONDARY_MESSAGE = "Accessed from this later case group.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }

    List<CaseGroupTree> caseGroups = ((SwitchTree) tree).cases();
    for (int index = 0; index < caseGroups.size(); index++) {
      CaseGroupTree caseGroup = caseGroups.get(index);
      if (!caseGroup.labels().get(0).isFallThrough()) {
        continue;
      }
      for (StatementTree statement : caseGroup.body()) {
        if (statement instanceof VariableTree variable) {
          reportIfAccessedFromLaterGroup(variable, caseGroups, index + 1);
        }
      }
    }
  }

  private void reportIfAccessedFromLaterGroup(VariableTree variable, List<CaseGroupTree> caseGroups, int firstLaterGroup) {
    Symbol symbol = variable.symbol();
    if (symbol.isUnknown()) {
      return;
    }

    List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    for (int index = firstLaterGroup; index < caseGroups.size(); index++) {
      IdentifierTree firstAccess = firstAccessTo(symbol, caseGroups.get(index));
      if (firstAccess != null) {
        secondaries.add(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, firstAccess));
      }
    }
    if (!secondaries.isEmpty()) {
      reportIssue(variable.simpleName(), PRIMARY_MESSAGE, secondaries, null);
    }
  }

  private static IdentifierTree firstAccessTo(Symbol symbol, CaseGroupTree caseGroup) {
    FirstSymbolAccessVisitor visitor = new FirstSymbolAccessVisitor(symbol);
    visitor.scanBody(caseGroup.body());
    return visitor.firstAccess;
  }

  private static class FirstSymbolAccessVisitor extends BaseTreeVisitor {

    private final Symbol symbol;
    private IdentifierTree firstAccess;

    private FirstSymbolAccessVisitor(Symbol symbol) {
      this.symbol = symbol;
    }

    private void scanBody(List<? extends Tree> trees) {
      scan(trees);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (firstAccess == null && tree.symbol().equals(symbol)) {
        firstAccess = tree;
      }
    }
  }
}
