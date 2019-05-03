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
package org.sonar.java.checks.synchronization;

import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.sonar.plugins.java.api.tree.Tree.Kind.EQUAL_TO;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IDENTIFIER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IF_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.MEMBER_SELECT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SYNCHRONIZED_STATEMENT;

@Rule(key = "S2168")
public class DoubleCheckedLockingCheck extends IssuableSubscriptionVisitor {

  private Deque<IfFieldEqNull> ifFieldStack = new LinkedList<>();
  private Deque<CriticalSection> synchronizedStmtStack = new LinkedList<>();
  private boolean methodIsSynchronized;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(IF_STATEMENT, SYNCHRONIZED_STATEMENT, METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    isIfFieldEqNull(tree).ifPresent(ifFieldEqNull -> {
      ifFieldStack.push(ifFieldEqNull);
      visitIfStatement(ifFieldEqNull.ifTree);
    });
    if (tree.is(SYNCHRONIZED_STATEMENT)) {
      CriticalSection criticalSection = new CriticalSection((SynchronizedStatementTree) tree, ifFieldStack.size());
      synchronizedStmtStack.push(criticalSection);
    }
    if (tree.is(METHOD)) {
      methodIsSynchronized = ModifiersUtils.hasModifier(((MethodTree) tree).modifiers(), Modifier.SYNCHRONIZED);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    isIfFieldEqNull(tree).ifPresent(cl -> ifFieldStack.pop());
    if (tree.is(SYNCHRONIZED_STATEMENT)) {
      synchronizedStmtStack.pop();
    }
  }

  private static Optional<IfFieldEqNull> isIfFieldEqNull(Tree tree) {
    if (!tree.is(IF_STATEMENT)) {
      return Optional.empty();
    }
    IfStatementTree ifTree = (IfStatementTree) tree;
    if (!ifTree.condition().is(EQUAL_TO)) {
      return Optional.empty();
    }
    BinaryExpressionTree eqRelation = (BinaryExpressionTree) ifTree.condition();
    if (eqRelation.rightOperand().is(NULL_LITERAL)) {
      return isField(eqRelation.leftOperand()).map(f -> new IfFieldEqNull(ifTree, f));
    }
    if (eqRelation.leftOperand().is(NULL_LITERAL)) {
      return isField(eqRelation.rightOperand()).map(f -> new IfFieldEqNull(ifTree, f));
    }
    return Optional.empty();
  }

  private void visitIfStatement(IfStatementTree ifTree) {
    if (insideCriticalSection()) {
      Optional<IfFieldEqNull> parentIf = sameFieldAlreadyOnStack(ifFieldStack.peek());
      parentIf.ifPresent(pIf -> ifSynchronizedIfPattern(pIf, ifTree));
    }
  }

  private void ifSynchronizedIfPattern(IfFieldEqNull parentIf, IfStatementTree nestedIf) {
    if (thenStmtInitializeField(nestedIf.thenStatement(), parentIf.field)
      && !parentIf.field.isVolatile()
      && !methodIsSynchronized) {
      SyntaxToken synchronizedKeyword = synchronizedStmtStack.peek().synchronizedTree.synchronizedKeyword();
      reportIssue(synchronizedKeyword, "Remove this dangerous instance of double-checked locking.", createFlow(parentIf.ifTree, nestedIf), null);
    }
  }

  private static List<JavaFileScannerContext.Location> createFlow(IfStatementTree parentIf, IfStatementTree nestedIf) {
    return Stream.of(parentIf.condition(), nestedIf.condition())
      .map(c -> new JavaFileScannerContext.Location("Double-checked locking", c))
      .collect(Collectors.toList());
  }

  private boolean insideCriticalSection() {
    return !synchronizedStmtStack.isEmpty();
  }

  /**
   * Returns if statement which is above the critical section (synchronized) and has the same condition as nestedIf
   *
   */
  private Optional<IfFieldEqNull> sameFieldAlreadyOnStack(IfFieldEqNull nestedIf) {
    int aboveSynchronized = ifFieldStack.size() - synchronizedStmtStack.peek().ifStackDepth;
    return ifFieldStack.stream()
      .skip(aboveSynchronized)
      .filter(parentIf -> parentIf.field == nestedIf.field)
      .findFirst();
  }

  private static Optional<Symbol> isField(ExpressionTree expressionTree) {
    return symbolFromVariable(expressionTree)
      .filter(s -> s.isVariableSymbol() && s.owner().isTypeSymbol());
  }

  private static Optional<Symbol> symbolFromVariable(ExpressionTree variable) {
    if (variable.is(IDENTIFIER)) {
      return Optional.of(((IdentifierTree) variable).symbol());
    }
    if (variable.is(MEMBER_SELECT)) {
      return Optional.of(((MemberSelectExpressionTree) variable).identifier().symbol());
    }
    return Optional.empty();
  }

  private static boolean thenStmtInitializeField(StatementTree statementTree, Symbol field) {
    AssignmentVisitor visitor = new AssignmentVisitor(field);
    statementTree.accept(visitor);
    return visitor.assignmentToField;
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
      symbolFromVariable(variable)
        .filter(s -> s == field)
        .ifPresent(s -> assignmentToField = true);
    }
  }

  private static class IfFieldEqNull {
    private final IfStatementTree ifTree;
    private final Symbol field;

    private IfFieldEqNull(IfStatementTree ifTree, Symbol field) {
      this.ifTree = ifTree;
      this.field = field;
    }
  }

  private static class CriticalSection {
    SynchronizedStatementTree synchronizedTree;
    int ifStackDepth;

    public CriticalSection(SynchronizedStatementTree synchronizedTree, int ifStackDepth) {
      this.synchronizedTree = synchronizedTree;
      this.ifStackDepth = ifStackDepth;
    }
  }

}
