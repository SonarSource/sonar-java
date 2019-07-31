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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3064")
public class DoubleCheckedLockingAssignmentCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    IfStatementTree ifStatementTree = (IfStatementTree) tree;
    StatementTree body = getOnlyStatement(ifStatementTree);
    if (body == null || !body.is(Tree.Kind.SYNCHRONIZED_STATEMENT)) {
      return;
    }
    List<StatementTree> synchronizedBody = ((SynchronizedStatementTree) body).block().body();
    if (synchronizedBody.isEmpty()) {
      return;
    }
    Symbol symbol = nullCheck(ifStatementTree.condition());
    if (symbol == null || !symbol.owner().isTypeSymbol()) {
      return;
    }
    for (StatementTree statementTree : synchronizedBody) {
      if (statementTree.is(Tree.Kind.IF_STATEMENT)) {
        IfStatementTree secondIf = (IfStatementTree) statementTree;
        Symbol secondIfSymbol = nullCheck(secondIf.condition());
        if (symbol.equals(secondIfSymbol)) {
          checkUsageAfterAssignment(symbol, secondIf.thenStatement());
        }
      }
    }
  }

  private void checkUsageAfterAssignment(Symbol symbol, StatementTree thenStatement) {
    if (thenStatement.is(Tree.Kind.BLOCK)) {
      List<StatementTree> body = ((BlockTree) thenStatement).body();
      AssignmentExpressionTree foundAssignment = null;
      UsageVisitor usageVisitor = new UsageVisitor(symbol);
      for (StatementTree statementTree : body) {
        if (foundAssignment != null) {
          statementTree.accept(usageVisitor);
        }
        AssignmentExpressionTree assignment = isAssignmentToSymbol(statementTree, symbol);
        if (assignment != null) {
          foundAssignment = assignment;
        }
      }
      if (foundAssignment != null && !usageVisitor.usages.isEmpty()) {
        reportIssue(foundAssignment, "Fully initialize \"" + symbol.name() + "\" before assigning it.", usageVisitor.locations(), null);
      }
    }
  }

  @CheckForNull
  private AssignmentExpressionTree isAssignmentToSymbol(StatementTree statementTree, Symbol symbol) {
    if (!statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return null;
    }
    ExpressionTree expr = ((ExpressionStatementTree) statementTree).expression();
    if (expr.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) expr;
      if (symbol.equals(symbol(assignment.variable()))) {
        return assignment;
      }
    }
    return null;
  }

  static class UsageVisitor extends BaseTreeVisitor {

    private final Symbol symbol;
    List<IdentifierTree> usages = new ArrayList<>();

    public UsageVisitor(Symbol symbol) {
      this.symbol = symbol;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (symbol.equals(tree.symbol())) {
        usages.add(tree);
      }
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // cut the visit
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      // cut the visit
    }

    @Override
    public void visitMethod(MethodTree tree) {
      // cut the visit
    }

    List<JavaFileScannerContext.Location> locations() {
      return usages.stream()
        .map(u -> new JavaFileScannerContext.Location("Usage after assignment", u))
        .collect(Collectors.toList());
    }
  }

  @CheckForNull
  private static StatementTree getOnlyStatement(IfStatementTree ifStatementTree) {
    StatementTree thenStatement = ifStatementTree.thenStatement();
    if (thenStatement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return thenStatement;
    }
    if (thenStatement.is(Tree.Kind.BLOCK)) {
      BlockTree blockTree = (BlockTree) thenStatement;
      if (blockTree.body().size() == 1) {
        return blockTree.body().get(0);
      }
    }
    return null;
  }

  @CheckForNull
  private static Symbol nullCheck(ExpressionTree tree) {
    if (!tree.is(Tree.Kind.EQUAL_TO)) {
      return null;
    }
    BinaryExpressionTree equalTo = (BinaryExpressionTree) tree;
    ExpressionTree lhs = equalTo.leftOperand();
    ExpressionTree rhs = equalTo.rightOperand();
    Symbol symbol = symbol(lhs);
    if (symbol != null && rhs.is(Tree.Kind.NULL_LITERAL)) {
      return symbol;
    }
    symbol = symbol(rhs);
    if (symbol != null && lhs.is(Tree.Kind.NULL_LITERAL)) {
      return symbol;
    }
    return null;
  }

  @CheckForNull
  private static Symbol symbol(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).symbol();
    }
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) tree).identifier().symbol();
    }
    return null;
  }

}
