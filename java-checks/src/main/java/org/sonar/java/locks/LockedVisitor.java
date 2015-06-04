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

import com.google.common.collect.Lists;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.symexecengine.ExecutionState;
import org.sonar.java.symexecengine.State;
import org.sonar.java.symexecengine.SymbolicExecutionCheck;
import org.sonar.java.symexecengine.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LockedVisitor extends SymbolicExecutionCheck {

  private static final String JAVA_LOCK = "java.util.concurrent.locks.Lock";

  private static final MethodInvocationMatcherCollection LOCK_INVOCATIONS = lockMethodInvocationMatcher();
  private static final MethodMatcher UNLOCK_INVOCATION = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK)).name("unlock");

  private static MethodInvocationMatcherCollection lockMethodInvocationMatcher() {
    return MethodInvocationMatcherCollection.create(
      MethodMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lock"),
      MethodMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("lockInterruptibly"),
      MethodMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK))
        .name("tryLock")
        .withNoParameterConstraint());
  }

  @Override
  public void initialize(ExecutionState executionState, MethodTree analyzedMethod, List<SymbolicValue> arguments) {
    for (Symbol field : getAccessibleLockFields(analyzedMethod.symbol())) {
      executionState.defineSymbol(field);
      executionState.createValueForSymbol(field, field.declaration());
    }
  }

  private List<Symbol> getAccessibleLockFields(Symbol.MethodSymbol symbol) {
    List<Symbol> symbols = Lists.newArrayList();
    Symbol owner = symbol.owner();
    while (owner.isTypeSymbol()) {
      Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) owner;
      for (Symbol member : typeSymbol.memberSymbols()) {
        if (member.isVariableSymbol() && member.type().isSubtypeOf(JAVA_LOCK)) {
          symbols.add(member);
        }
      }
      owner = owner.owner();
    }
    return symbols;
  }

  @Override
  protected void onExecutableElementInvocation(ExecutionState executionState, Tree tree, List<ExpressionTree> arguments) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
      ExpressionTree methodSelect = methodInvocation.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
        if (expression.is(Tree.Kind.IDENTIFIER)) {
          Symbol symbol = ((IdentifierTree) expression).symbol();
          if (LOCK_INVOCATIONS.anyMatch(methodInvocation)) {
            executionState.markValueAs(symbol, new LockState.Locked(methodInvocation));
          } else if (UNLOCK_INVOCATION.matches(methodInvocation)) {
            executionState.markValueAs(symbol, new LockState.Unlocked(methodInvocation));
          }
        }
      }
    }
  }

  private final Set<Tree> issueTree = new HashSet<>();

  @Override
  protected void onValueUnreachable(ExecutionState executionState, State state) {
    if (state instanceof LockState.Locked) {
      issueTree.addAll(state.reportingTrees());
    }
  }

  public Set<Tree> getIssueTrees() {
    return issueTree;
  }

}
