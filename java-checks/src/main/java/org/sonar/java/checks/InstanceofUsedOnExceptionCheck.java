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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1193")
public class InstanceofUsedOnExceptionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    CatchTree catchTree = ((CatchTree) tree);

    String caughtVariable = catchTree.parameter().simpleName().name();

    List<StatementTree> body = catchTree.block().body();
    if (body.stream().allMatch(statement -> statement.is(Tree.Kind.RETURN_STATEMENT, Tree.Kind.THROW_STATEMENT, Tree.Kind.IF_STATEMENT))) {
      reportSimpleInstanceOf(body, caughtVariable);
    }
  }

  private void reportSimpleInstanceOf(List<StatementTree> body, String caughtVariable) {
    List<ExpressionTree> conditions = body.stream()
      .filter(statement -> statement.is(Tree.Kind.IF_STATEMENT))
      .map(IfStatementTree.class::cast)
      .flatMap(InstanceofUsedOnExceptionCheck::getFollowingElseIf)
      .map(IfStatementTree::condition)
      .collect(Collectors.toList());

    if (conditions.stream().allMatch(cond -> cond.is(Tree.Kind.INSTANCE_OF) && isLeftOperandAndException((InstanceOfTree) cond, caughtVariable))) {
      conditions.stream()
        .map(InstanceOfTree.class::cast)
        .forEach(instanceOfTree ->
          reportIssue(instanceOfTree.instanceofKeyword(), "Replace the usage of the \"instanceof\" operator by a catch block."));
    }
  }

  private static boolean isLeftOperandAndException(InstanceOfTree instanceOfTree, String caughtVariable) {
    ExpressionTree expression = instanceOfTree.expression();
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      return caughtVariable.equals(((IdentifierTree) expression).name())
        && instanceOfTree.type().symbolType().isSubtypeOf("java.lang.Throwable");
    }
    return false;
  }

  private static Stream<IfStatementTree> getFollowingElseIf(IfStatementTree ifStatementTree) {
    List<IfStatementTree> ifStatements = new ArrayList<>();
    ifStatements.add(ifStatementTree);

    StatementTree elseStatement = ifStatementTree.elseStatement();
    while (elseStatement != null) {
      if (elseStatement.is(Tree.Kind.IF_STATEMENT)) {
        IfStatementTree elseIfStatement = (IfStatementTree) elseStatement;
        ifStatements.add(elseIfStatement);
        elseStatement = elseIfStatement.elseStatement();
      } else {
        elseStatement = null;
      }
    }

    return ifStatements.stream();
  }
}
