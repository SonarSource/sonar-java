/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "SwitchLastCaseIsDefaultCheck", repositoryKey = "squid")
@Rule(key = "S131")
public class SwitchLastCaseIsDefaultCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_LABEL_STRING = JavaKeyword.DEFAULT.getValue();
  
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    if (getDefaultLabel(switchStatementTree)) {
      if (!isSwitchOnEnum(switchStatementTree)) {
        reportIssue(switchStatementTree.switchKeyword(), "Add a default case to this switch.");
      } else if (missingCasesOfEnum(switchStatementTree)) {
        reportIssue(switchStatementTree.switchKeyword(), "Complete cases by adding the missing enum constants or add a default case to this switch.");
      }
    }
  }

  private static boolean getDefaultLabel(SwitchStatementTree switchStatementTree) {
    return allLabels(switchStatementTree).noneMatch(SwitchLastCaseIsDefaultCheck::isDefault);
  }

  private static boolean isDefault(CaseLabelTree caseLabelTree) {
    if (equalsDefaultKeyword(caseLabelTree.caseOrDefaultKeyword().text())) {
      return true;
    }
    return caseLabelTree.expressions().stream().anyMatch(expr -> expr.is(Tree.Kind.DEFAULT_PATTERN));
  }

  private static boolean equalsDefaultKeyword(String text) {
    return DEFAULT_LABEL_STRING.equals(text);
  }

  private static boolean isSwitchOnEnum(SwitchStatementTree switchStatementTree) {
    Symbol.TypeSymbol symbol = switchStatementTree.expression().symbolType().symbol();
    return symbol.isEnum() || symbol.isUnknown();
  }

  private static boolean missingCasesOfEnum(SwitchStatementTree switchStatementTree) {
    return numberConstants(switchStatementTree) > allExpressions(switchStatementTree).count();
  }

  private static Stream<CaseLabelTree> allLabels(SwitchStatementTree switchStatementTree) {
    return switchStatementTree.cases().stream().flatMap(caseGroup -> caseGroup.labels().stream());
  }

  private static Stream<ExpressionTree> allExpressions(SwitchStatementTree switchStatementTree) {
    return allLabels(switchStatementTree).flatMap(caseLabel -> caseLabel.expressions().stream());
  }

  private static long numberConstants(SwitchStatementTree switchStatementTree) {
    return switchStatementTree.expression().symbolType().symbol().memberSymbols().stream()
      .filter(Symbol::isVariableSymbol)
      .filter(Symbol::isEnum)
      .count();
  }
}
