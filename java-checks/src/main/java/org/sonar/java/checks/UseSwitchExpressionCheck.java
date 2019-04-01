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
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.RETURN_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.THROW_STATEMENT;

@Rule(key = "S5194")
public class UseSwitchExpressionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SWITCH_STATEMENT);
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
    SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
    Symbol switchAssigningVariable = isSwitchAssigningVariable(switchStatementTree);
    if (switchAssigningVariable != null) {
      reportIssue(switchStatementTree.switchKeyword(), "Use expression switch to set value of '" + switchAssigningVariable.name() + "'.");
    } else if (isSwitchReturningValue(switchStatementTree)) {
      reportIssue(switchStatementTree.switchKeyword(), "Use expression switch to return the value.");
    }
  }

  private static Symbol isSwitchAssigningVariable(SwitchStatementTree switchStatementTree) {
    Symbol switchAssigningVariable = null;
    for (CaseGroupTree caseGroupTree : switchStatementTree.cases()) {
      if (hasSingleStatement(caseGroupTree.body(), THROW_STATEMENT)) {
        continue;
      }
      Symbol variable = assigningVariable(caseGroupTree.body());
      if (variable == null) {
        return null;
      }
      if (switchAssigningVariable == null) {
        switchAssigningVariable = variable;
      } else if (switchAssigningVariable != variable) {
        return null;
      }
    }
    return switchAssigningVariable;
  }

  private static boolean isSwitchReturningValue(SwitchStatementTree switchStatementTree) {
    for (CaseGroupTree caseGroupTree : switchStatementTree.cases()) {
      if (hasSingleStatement(caseGroupTree.body(), THROW_STATEMENT, RETURN_STATEMENT)) {
        continue;
      }
      return false;
    }
    return true;
  }

  private static Symbol assigningVariable(List<StatementTree> body) {
    if (body.size() != 2) {
      return null;
    }
    if (!body.get(0).is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return null;
    }
    if (!body.get(1).is(Tree.Kind.BREAK_STATEMENT)) {
      return null;
    }
    ExpressionTree expression = ((ExpressionStatementTree) body.get(0)).expression();
    if (!expression.is(Tree.Kind.ASSIGNMENT)) {
      return null;
    }
    ExpressionTree variable = ((AssignmentExpressionTree) expression).variable();
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) variable).symbol();
    }
    if (variable.is(MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier().symbol();
    }
    return null;
  }

  private static boolean hasSingleStatement(List<StatementTree> body, Tree.Kind... statement) {
    return body.size() == 1 && body.get(0).is(statement);
  }

}
