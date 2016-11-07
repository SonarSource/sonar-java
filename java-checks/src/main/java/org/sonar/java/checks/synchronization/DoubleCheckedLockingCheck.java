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
package org.sonar.java.checks.synchronization;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static org.sonar.plugins.java.api.tree.Tree.Kind.EQUAL_TO;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IDENTIFIER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IF_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SYNCHRONIZED_STATEMENT;

@Rule(key = "S2168")
public class DoubleCheckedLockingCheck extends IssuableSubscriptionVisitor {

  private Deque<CheckedLocking> ifConditionSymbolStack = new LinkedList<>();
  private Deque<Tree> synchronizedStmtStack = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(IF_STATEMENT, SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (isIfFieldEqNullTree(tree)) {
      IfStatementTree ifTree = (IfStatementTree) tree;
      ifConditionSymbolStack.push(new CheckedLocking(ifTree));
      visitIfStatement(ifTree);
    }
    if (tree.is(SYNCHRONIZED_STATEMENT)) {
      synchronizedStmtStack.push(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (isIfFieldEqNullTree(tree)) {
      ifConditionSymbolStack.pop();
    }
    if (tree.is(SYNCHRONIZED_STATEMENT)) {
      synchronizedStmtStack.pop();
    }
  }

  private static boolean isIfFieldEqNullTree(Tree tree) {
    if (!tree.is(IF_STATEMENT)) {
      return false;
    }
    IfStatementTree ifTree = (IfStatementTree) tree;
    if (!ifTree.condition().is(EQUAL_TO)) {
      return false;
    }
    BinaryExpressionTree eqRelation = (BinaryExpressionTree) ifTree.condition();
    return (isField(eqRelation.leftOperand()) && eqRelation.rightOperand().is(NULL_LITERAL))
      || (isField(eqRelation.rightOperand()) && eqRelation.leftOperand().is(NULL_LITERAL));
  }

  private void visitIfStatement(IfStatementTree ifTree) {
    if (insideCriticalSection()) {
      CheckedLocking parentIf = sameFieldAlreadyOnStack(ifConditionSymbolStack.peek());
      if (parentIf.ifTree == ifTree) {
        return;
      }
      if (thenStmtInitializeField(ifTree.thenStatement(), parentIf.field)
        && !parentIf.field.isVolatile()
        && !isAssignedAtomically(parentIf.field)) {
        ImmutableList<JavaFileScannerContext.Location> flow = ImmutableList.of(new JavaFileScannerContext.Location("Double-checked locking", ifTree.ifKeyword()));
        reportIssue(parentIf.ifTree.ifKeyword(), "Remove this dangerous instance of double-checked locking.", flow, null);
      }
    }
  }

  private boolean insideCriticalSection() {
    return !synchronizedStmtStack.isEmpty();
  }

  /**
   * Returns if statement which has the same condition as nestedIf , or nestedIf if there is no such other
   * if statement on the stack
   */
  private CheckedLocking sameFieldAlreadyOnStack(CheckedLocking nestedIf) {
    return ifConditionSymbolStack.stream()
      .skip(1)
      .filter(parentIf -> parentIf.field == nestedIf.field)
      .findFirst()
      .orElse(nestedIf);
  }

  private static boolean isField(ExpressionTree expressionTree) {
    Symbol symbol = symbolFromVariable(expressionTree);
    if (symbol == null) {
      return false;
    }
    return symbol.isVariableSymbol() && symbol.owner().isTypeSymbol();
  }

  @CheckForNull
  private static Symbol symbolFromVariable(ExpressionTree variable) {
    if (variable.is(IDENTIFIER)) {
      return ((IdentifierTree) variable).symbol();
    }
    if (variable.is(MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier().symbol();
    }
    return null;
  }

  private static boolean thenStmtInitializeField(StatementTree statementTree, Symbol field) {
    AssignmentVisitor visitor = new AssignmentVisitor(field);
    statementTree.accept(visitor);
    return visitor.assignmentToField;
  }

  private static boolean isAssignedAtomically(Symbol field) {
    Type fieldType = field.type();
    if (fieldType.isUnknown() || fieldType.symbol().isInterface() || fieldType.symbol().isAbstract()) {
      return false;
    }
    if (fieldType.isPrimitive()) {
      return true;
    }
    Collection<Symbol> members = fieldType.symbol().memberSymbols();
    return members.stream()
      .noneMatch(m -> m.isVariableSymbol() && m.type().isPrimitive() && !m.isFinal());
  }

  private static class AssignmentVisitor extends BaseTreeVisitor {

    private boolean assignmentToField;
    private Symbol field;

    AssignmentVisitor(Symbol field) {
      this.field = field;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree assignmentTree) {
      ExpressionTree variable = assignmentTree.variable();
      Symbol symbol = symbolFromVariable(variable);
      if (field == symbol) {
        assignmentToField = true;
      }
    }
  }

  private static class CheckedLocking {
    private final IfStatementTree ifTree;
    private final Symbol field;

    private CheckedLocking(IfStatementTree ifTree) {
      this.ifTree = ifTree;
      this.field = fieldFromEqCondition(ifTree.condition());
    }

    private static Symbol fieldFromEqCondition(ExpressionTree condition) {
      if (!condition.is(EQUAL_TO)) {
        return null;
      }
      BinaryExpressionTree eqRelation = (BinaryExpressionTree) condition;
      if (isField(eqRelation.leftOperand())) {
        return symbolFromVariable(eqRelation.leftOperand());
      }
      if (isField(eqRelation.rightOperand())) {
        return symbolFromVariable(eqRelation.rightOperand());
      }
      return null;
    }
  }

}
