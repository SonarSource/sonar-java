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
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1126")
public class ReturnOfBooleanExpressionsCheck extends IssuableSubscriptionVisitor {


  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    IfStatementTree ifStatementTree = (IfStatementTree) tree;
    StatementTree elseStatementOrNextStatement = getStatementTree(ifStatementTree);
    StatementTree thenStatement = ifStatementTree.thenStatement();

    if (hasOneReturnBoolean(elseStatementOrNextStatement) && hasOneReturnBoolean(thenStatement)) {
      reportIssue(ifStatementTree.ifKeyword(), "Replace this if-then-else statement by a single return statement.");
    } else {
      Optional<MethodInvocationTree> elseMIT = getMethodInvocation(elseStatementOrNextStatement);
      Optional<MethodInvocationTree> thenMIT = getMethodInvocation(thenStatement);
      if (elseMIT.isPresent()
        && thenMIT.isPresent()
        && areAllSyntacticallyEquivalentExceptBoolean(elseMIT.get(), thenMIT.get())) {
        reportIssue(ifStatementTree.ifKeyword(), "Replace this if-then-else statement by a single method invocation.");
      }
    }
  }

  private static StatementTree getStatementTree(IfStatementTree ifStatementTree) {
    StatementTree elseStatementOrNextStatement = ifStatementTree.elseStatement();
    if (elseStatementOrNextStatement == null) {
      JavaTree parent = (JavaTree) ifStatementTree.parent();
      List<Tree> children = parent.getChildren();
      int indexOfIf = children.indexOf(ifStatementTree);
      if (indexOfIf < children.size() - 1) {
        // Defensive, this condition should always be true as if necessarily followed by a statement or a token.
        Tree next = children.get(indexOfIf + 1);
        if(!next.is(Kind.TOKEN)) {
          elseStatementOrNextStatement = (StatementTree) next;
        }
      }
    }
    return elseStatementOrNextStatement;
  }

  private static boolean hasOneReturnBoolean(@Nullable StatementTree statementTree) {
    if (statementTree == null) {
      return false;
    }
    if (statementTree.is(Kind.BLOCK)) {
      BlockTree block = (BlockTree) statementTree;
      return block.body().size() == 1 && isReturnBooleanLiteral(block.body().get(0));
    }
    return isReturnBooleanLiteral(statementTree);
  }

  private static boolean isReturnBooleanLiteral(StatementTree statementTree) {
    if (statementTree.is(Kind.RETURN_STATEMENT)) {
      ExpressionTree expression = ((ReturnStatementTree) statementTree).expression();
      return expression != null && expression.is(Tree.Kind.BOOLEAN_LITERAL);
    }
    return false;
  }

  private static Optional<MethodInvocationTree> getMethodInvocation(@Nullable StatementTree statementTree) {
    if (statementTree == null) {
      return Optional.empty();
    }
    Tree newTree = statementTree;
    if (newTree.is(Tree.Kind.BLOCK)) {
      List<StatementTree> body = ((BlockTree) newTree).body();
      if (body.size() != 1) {
        return Optional.empty();
      }
      newTree = body.get(0);
    }
    ExpressionTree expressionTree = null;
    if (newTree.is(Tree.Kind.RETURN_STATEMENT)) {
      expressionTree = ((ReturnStatementTree) newTree).expression();
    } else if (newTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      expressionTree = ((ExpressionStatementTree) newTree).expression();
    }
    if (expressionTree != null) {
      expressionTree = ExpressionUtils.skipParentheses(expressionTree);
      if (expressionTree.is(Kind.METHOD_INVOCATION)) {
        return Optional.of((MethodInvocationTree) expressionTree);
      }
    }
    return Optional.empty();
  }

  private static Tree firstNonParenthesesParent(Tree tree) {
    Tree skip = tree.parent();
    while (skip.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      skip = skip.parent();
    }
    return skip;
  }

  private static boolean areAllSyntacticallyEquivalentExceptBoolean(MethodInvocationTree mit1, MethodInvocationTree mit2) {
    if (firstNonParenthesesParent(mit1).kind() != firstNonParenthesesParent(mit2).kind()) {
      // requires to have on both side a return statement, or on both side an expression statement.
      return false;
    }
    if (!SyntacticEquivalence.areEquivalent(mit1.methodSelect(), mit2.methodSelect())) {
      return false;
    }
    List<ExpressionTree> mit1Args = mit1.arguments();
    List<ExpressionTree> mit2Args = mit2.arguments();
    if (mit1Args.size() != mit2Args.size()) {
      return false;
    }
    boolean containsBooleanLiteral = false;
    for (int i = 0; i < mit1Args.size(); i++) {
      ExpressionTree arg1 = ExpressionUtils.skipParentheses(mit1Args.get(i));
      ExpressionTree arg2 = ExpressionUtils.skipParentheses(mit2Args.get(i));
      boolean arg1IsBooleanLiteral = arg1.is(Tree.Kind.BOOLEAN_LITERAL);
      boolean arg2IsBooleanLiteral = arg2.is(Tree.Kind.BOOLEAN_LITERAL);
      if (SyntacticEquivalence.areEquivalent(arg1, arg2)) {
        containsBooleanLiteral |= arg1IsBooleanLiteral;
      } else {
        if (!(arg1IsBooleanLiteral && arg2IsBooleanLiteral)) {
          return false;
        }
        containsBooleanLiteral = true;
      }
    }
    return containsBooleanLiteral;
  }
}
