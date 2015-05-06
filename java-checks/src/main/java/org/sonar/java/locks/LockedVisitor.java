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

import java.util.List;

public class LockedVisitor extends DataFlowVisitor {

  private static final String JAVA_LOCK = "java.util.concurrent.locks.Lock";

  private static final MethodInvocationMatcherCollection LOCK_INVOCATIONS = lockMethodInvocationMatcher();
  private static final MethodInvocationMatcher UNLOCK_INVOCATION = MethodInvocationMatcher.create().typeDefinition(TypeCriteria.subtypeOf(JAVA_LOCK)).name("unlock");

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

  public LockedVisitor(Symbol.MethodSymbol analyzedMethod) {
    super();
    for (Symbol field : getAccessibleLockFields(analyzedMethod)) {
      executionState.defineSymbol(field);
      executionState.createValueForSymbol(field, field.declaration());
    }
  }

  private List<Symbol> getAccessibleLockFields(Symbol.MethodSymbol symbol) {
    List<Symbol> symbols = Lists.newArrayList();
    Symbol owner =  symbol.owner();
    while (owner.isTypeSymbol()) {
      Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol) owner;
      for (Symbol member : typeSymbol.memberSymbols()) {
        if(member.isVariableSymbol() && member.type().isSubtypeOf(JAVA_LOCK)) {
          symbols.add(member);
        }
      }
      owner = owner.owner();
    }
    return symbols;
  }


  @Override
  protected boolean isSymbolRelevant(Symbol symbol) {
    return symbol.type().isSubtypeOf(JAVA_LOCK);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    ExpressionTree methodSelect = tree.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        Symbol symbol = ((IdentifierTree) expression).symbol();
        if (LOCK_INVOCATIONS.anyMatch(tree)) {
          executionState.markValueAs(symbol, new LockState.Locked(tree));
        } else if (UNLOCK_INVOCATION.matches(tree)) {
          executionState.markValueAs(symbol, new LockState.Unlocked(tree));
        }
      }
    }
  }
}
