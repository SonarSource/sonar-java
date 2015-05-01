/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.locks;

import org.sonar.java.checks.SubscriptionBaseVisitor;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.symexecengine.DataFlowVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

public class LockedVisitor extends DataFlowVisitor {

  private static final String JAVA_LOCK = "java.util.concurrent.locks.Lock";

  private static final MethodInvocationMatcherCollection LOCK_INVOCATIONS = lockMethodInvocationMatcher();
  private static final MethodInvocationMatcher UNLOCK_INVOCATION = MethodInvocationMatcher.create().typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK)).name("unlock");

  public LockedVisitor(SubscriptionBaseVisitor check) {
    super(check);
  }

  private static MethodInvocationMatcherCollection lockMethodInvocationMatcher() {
    return MethodInvocationMatcherCollection.create(
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lock"),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lockInterruptibly"),
      MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("tryLock")
        .withNoParameterConstraint());
  }

  @Override
  protected boolean isSymbolRelevant(Symbol symbol) {
    return symbol.type().isSubtypeOf(JAVA_LOCK);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (LOCK_INVOCATIONS.anyMatch(tree)) {
      Symbol symbol = extractInvokedOnSymbol(tree.methodSelect());
      if (symbol != null) {
        executionState.markValueAs(symbol, new LockState.Locked(tree));
      }
    } else if (UNLOCK_INVOCATION.matches(tree)) {
      ExpressionTree methodSelect = tree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
        if (expression.is(Tree.Kind.IDENTIFIER)) {
          executionState.markValueAs(((IdentifierTree) expression).symbol(), new LockState.Unlocked(tree));
        }
      }
    }
  }

  @CheckForNull
  private Symbol extractInvokedOnSymbol(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      return extractSymbol(((MemberSelectExpressionTree) expressionTree).expression());
    } else if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expressionTree).symbol().owner();
    }
    return null;
  }

  @CheckForNull
  private Symbol extractSymbol(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expressionTree).identifier().symbol();
    } else if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expressionTree).symbol();
    }
    return null;
  }

}
