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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1301")
public class SwitchAtLeastThreeCasesCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;

    int count = 0;
    boolean hasDefault = false;
    for (CaseGroupTree caseGroup : switchStatementTree.cases()) {
      // whenever there is a type, record or guarded pattern, it would decrease readability to replace the switch by if
      // so we don't raise an issue
      if (hasLabelWithAllowedPattern(caseGroup)) {
        return;
      }
      count += totalLabelCount(caseGroup);
      if (containsDefaultLabel(caseGroup)) {
        hasDefault = true;
      }
    }

    if (count >= 3) {
      return;
    }

    // Do not suggest replacing exhaustive switches over small enums.
    // It does not aid readability and can lead to error-prone code when
    // "->" switch is used (adding new enum constant would cause compilation error).
    Symbol.TypeSymbol typeSymbol = switchStatementTree.expression().symbolType().symbol();
    if (typeSymbol.isEnum() && !hasDefault && enumConstantCount(typeSymbol) == count) {
      return;
    }

    reportIssue(switchStatementTree.switchKeyword(), "Replace this \"switch\" statement by \"if\" statements to increase readability.");
  }

  /**
   * Count labels, taking into account Java 14 multi-label switch.
   * For example, here we have 4 labels:
   * <pre>
   *   case "Monday", "Tuesday":
   *   case "Wednesday:
   *   default: // considered 1 label
   * </pre>
   */
  private static int totalLabelCount(CaseGroupTree caseGroup) {
    int total = 0;
    for (CaseLabelTree label: caseGroup.labels()) {
      int sz = label.expressions().size();
      // `default` does not have any expressions, but we consider it 1 label.
      total += sz > 0 ? sz : 1;
    }
    return total;
  }

  private static boolean containsDefaultLabel(CaseGroupTree caseGroup) {
    for (CaseLabelTree label: caseGroup.labels()) {
      if ("default".equals(label.caseOrDefaultKeyword().text())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Number of constants declared in the enum. Note, that it excludes fields and methods.
   */
  private static long enumConstantCount(Symbol.TypeSymbol typeSymbol) {
    return typeSymbol.memberSymbols().stream()
      .filter(s -> s.isVariableSymbol() && s.isEnum())
      .count();
  }

  private static boolean hasLabelWithAllowedPattern(CaseGroupTree caseGroupTree) {
    return caseGroupTree.labels().stream()
      .flatMap(label -> label.expressions().stream())
      .anyMatch(expression -> expression.is(Tree.Kind.TYPE_PATTERN, Tree.Kind.RECORD_PATTERN, Tree.Kind.GUARDED_PATTERN));
  }

}
