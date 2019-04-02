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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.BLOCK;
import static org.sonar.plugins.java.api.tree.Tree.Kind.BREAK_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.EXPRESSION_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.RETURN_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SWITCH_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.THROW_STATEMENT;

@Rule(key = "S5194")
public class UseSwitchExpressionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(SWITCH_STATEMENT);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava12Compatible();
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    SwitchStatementTree switchTree = (SwitchStatementTree) tree;
    Symbol switchAssigningVariable = isSwitchAssigningVariable(switchTree);
    if (switchAssigningVariable != null) {
      reportIssue(switchTree.switchKeyword(), "Use \"switch\" expression to set value of \"" + switchAssigningVariable.name() + "\".");
    } else if (isSwitchReturningValue(switchTree)) {
      reportIssue(switchTree.switchKeyword(), "Use \"switch\" expression to return the value from method.");
    }
  }

  @CheckForNull
  private static Symbol isSwitchAssigningVariable(SwitchStatementTree switchStatementTree) {
    Set<Symbol> assignedVariables = switchStatementTree.cases().stream()
      .filter(caseGroup -> !hasSingleStatement(caseGroup.body(), THROW_STATEMENT))
      .map(UseSwitchExpressionCheck::assigningVariable)
      .collect(Collectors.toSet());
    return assignedVariables.size() == 1 ? assignedVariables.iterator().next() : null;
  }

  private static boolean isSwitchReturningValue(SwitchStatementTree switchStatementTree) {
    return switchStatementTree.cases().stream()
      .map(CaseGroupTree::body)
      .allMatch(body -> hasSingleStatement(body, THROW_STATEMENT, RETURN_STATEMENT));
  }

  @CheckForNull
  private static Symbol assigningVariable(CaseGroupTree caseGroup) {
    List<StatementTree> body = caseGroup.body();
    if (isArrowOrBreaks(caseGroup)) {
      return variableFromAssignment(body);
    }
    return null;
  }

  @CheckForNull
  private static Symbol variableFromAssignment(List<StatementTree> body) {
    if (body.isEmpty()) {
      return null;
    }
    StatementTree statementTree = body.get(0);
    if (statementTree.is(EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statementTree).expression();
      if (expression.is(Tree.Kind.ASSIGNMENT)) {
        Symbol variable = getVariableSymbol((AssignmentExpressionTree) expression);
        if (variable != null && !variable.isUnknown()) {
          return variable;
        }
      }
    }
    if (statementTree.is(BLOCK)) {
      return variableFromAssignment(((BlockTree) statementTree).body());
    }
    return null;
  }

  private static boolean isArrowOrBreaks(CaseGroupTree caseGroup) {
    List<StatementTree> body = caseGroup.body();
    return !caseGroup.labels().get(0).isFallThrough() ||
      hasBreak(body) ||
      (hasSingleStatement(body, BLOCK) && hasBreak(((BlockTree) body.get(0)).body()));
  }

  private static boolean hasBreak(List<StatementTree> body) {
    return body.size() == 2 && body.get(1).is(BREAK_STATEMENT);
  }

  @CheckForNull
  private static Symbol getVariableSymbol(AssignmentExpressionTree expression) {
    ExpressionTree variable = expression.variable();
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) variable).symbol();
    }
    if (variable.is(MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier().symbol();
    }
    return null;
  }

  private static boolean hasSingleStatement(List<StatementTree> body, Tree.Kind... statement) {
    if (body.size() != 1) {
      return false;
    }
    StatementTree onlyStatement = body.get(0);
    return onlyStatement.is(statement) ||
      (onlyStatement.is(BLOCK) && hasSingleStatement(((BlockTree) onlyStatement).body(), statement));
  }

}
