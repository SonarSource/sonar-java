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
package org.sonar.java.se.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValueFactory;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2222",
  name = "Locks should be released",
  priority = Priority.CRITICAL,
  tags = {"bug", "cwe", "multi-threading"})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("20min")
public class LocksNotUnlockedCheck extends SECheck {

  private enum Status {
    LOCKED, UNLOCKED;
  }

  private static class TryLockSymbolicValue extends SymbolicValue {

    private final SymbolicValue operand;
    private final Tree syntaxNode;

    public TryLockSymbolicValue(final int id, final SymbolicValue operand, final Tree syntaxNode) {
      super(id);
      this.operand = operand;
      this.syntaxNode = syntaxNode;
    }

    @Override
    public boolean references(SymbolicValue other) {
      return operand.equals(other) || operand.references(other);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      if (BooleanConstraint.TRUE.equals(booleanConstraint)) {
        return ImmutableList.of(programState.addConstraint(operand, new ObjectConstraint(false, false, syntaxNode, Status.LOCKED)));
      } else {
        return ImmutableList.of(programState.addConstraint(operand, new ObjectConstraint(syntaxNode, Status.UNLOCKED)));
      }
    }

    @Override
    public String toString() {
      return super.toString() + ".tryLock()";
    }
  }

  private static class TryLockSymbolicValueFactory implements SymbolicValueFactory {

    private final SymbolicValue operand;

    TryLockSymbolicValueFactory(final SymbolicValue operand) {
      this.operand = operand;
    }

    @Override
    public SymbolicValue createSymbolicValue(int id, Tree syntaxNode) {
      return new TryLockSymbolicValue(id, operand, syntaxNode);
    }

  }

  private static class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private static final String LOCK = "java.util.concurrent.locks.Lock";
    private static final String LOCK_METHOD_NAME = "lock";
    private static final String TRY_LOCK_METHOD_NAME = "tryLock";
    private static final String UNLOCK_METHOD_NAME = "unlock";

    private final ConstraintManager constraintManager;

    public PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree syntaxNode) {
      if (syntaxNode.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) syntaxNode.methodSelect();
        final ExpressionTree expression = memberSelect.expression();
        if (expression.is(Tree.Kind.IDENTIFIER) && expression.symbolType().isSubtypeOf(LOCK)) {
          final String methodName = memberSelect.identifier().name();
          visitMethodInvocationWithIdentifierTarget(methodName, (IdentifierTree) expression);
        }
      }
    }

    private void visitMethodInvocationWithIdentifierTarget(final String methodName, final IdentifierTree target) {
      if (!isMemberSelectActingOnField(target)) {
        final SymbolicValue symbolicValue = programState.getValue(target.symbol());
        if (LOCK_METHOD_NAME.equals(methodName)) {
          programState = programState.addConstraint(symbolicValue, new ObjectConstraint(false, false, target, Status.LOCKED));
        } else if (TRY_LOCK_METHOD_NAME.equals(methodName)) {
          constraintManager.setValueFactory(new TryLockSymbolicValueFactory(symbolicValue));
          programState = programState.addConstraint(symbolicValue, new ObjectConstraint(false, false, target, Status.LOCKED));
        } else if (UNLOCK_METHOD_NAME.equals(methodName)) {
          programState = programState.addConstraint(symbolicValue, new ObjectConstraint(target, Status.UNLOCKED));
        }
      }
    }

    private static boolean isMemberSelectActingOnField(IdentifierTree expression) {
      final Symbol symbol = expression.symbol();
      return ProgramState.isField(symbol);
    }
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    final PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    final List<ObjectConstraint> constraints = context.getState().getFieldConstraints(Status.LOCKED);
    for (ObjectConstraint constraint : constraints) {
      Tree syntaxNode = constraint.syntaxNode();
      context.reportIssue(syntaxNode, this, "Unlock \"" + syntaxNode.toString() + "\" along all executions paths of this method.");
    }
  }
}
