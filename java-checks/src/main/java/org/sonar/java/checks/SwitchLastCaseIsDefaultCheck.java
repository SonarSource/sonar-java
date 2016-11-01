/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Rule(key = "SwitchLastCaseIsDefaultCheck")
@RspecKey("S131")
public class SwitchLastCaseIsDefaultCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    Optional<CaseLabelTree> defaultLabel = getDefaultLabel(switchStatementTree);
    if (!defaultLabel.isPresent()) {
      if (!isSwitchOnEnum(switchStatementTree)) {
        reportIssue(switchStatementTree.switchKeyword(), "Add a default case to this switch.");
      } else if (missingCasesOfEnum(switchStatementTree)) {
        reportIssue(switchStatementTree.switchKeyword(), "Complete cases by adding the missing enum constants or add a default case to this switch.");
      }
    } else if (!defaultLabel.get().equals(getLastLabel(switchStatementTree))) {
      reportIssue(defaultLabel.get(), "Move this default to the end of the switch.");
    }
  }

  private static Optional<CaseLabelTree> getDefaultLabel(SwitchStatementTree switchStatementTree) {
    return allLabels(switchStatementTree).stream()
      .filter(SwitchLastCaseIsDefaultCheck::isDefault)
      .findAny();
  }

  private static boolean isDefault(CaseLabelTree caseLabelTree) {
    return JavaKeyword.DEFAULT.getValue().equals(caseLabelTree.caseOrDefaultKeyword().text());
  }

  private static boolean isSwitchOnEnum(SwitchStatementTree switchStatementTree) {
    return switchStatementTree.expression().symbolType().symbol().isEnum();
  }

  private static boolean missingCasesOfEnum(SwitchStatementTree switchStatementTree) {
    return numberConstants(switchStatementTree) > allLabels(switchStatementTree).size();
  }

  private static List<CaseLabelTree> allLabels(SwitchStatementTree switchStatementTree) {
    return switchStatementTree.cases().stream()
      .flatMap(caseGroup -> caseGroup.labels().stream())
      .collect(Collectors.toList());
  }

  private static long numberConstants(SwitchStatementTree switchStatementTree) {
    Symbol.TypeSymbol enumType = switchStatementTree.expression().symbolType().symbol();
    return enumType.memberSymbols().stream()
      .filter(Symbol::isVariableSymbol)
      .filter(Symbol::isEnum)
      .count();
  }

  private static CaseLabelTree getLastLabel(SwitchStatementTree switchStatementTree) {
    List<CaseGroupTree> caseGroups = switchStatementTree.cases();
    List<CaseLabelTree> lastCaseLabels = caseGroups.get(caseGroups.size() - 1).labels();
    return lastCaseLabels.get(lastCaseLabels.size() - 1);
  }
}
