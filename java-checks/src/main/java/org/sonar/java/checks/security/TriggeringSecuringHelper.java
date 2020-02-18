/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Optional;
import java.util.function.Predicate;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.ExpressionUtils.extractIdentifierSymbol;
import static org.sonar.java.model.ExpressionUtils.getAssignedSymbol;

/**
 * Helper class implementing the following behavior:
 *
 * 1. Test if a {@link MethodInvocationTree} matches the triggering method matcher given to constructor.
 * 2. If found, get enclosing method (if any).
 * 3. Visit the body of the enclosing method, calling {@link #processSecuringMethodInvocation(MethodInvocationTree)} for each method invocation.
 * 4. At the end of the visit, return true if the variable was secured inside the body of the enclosing method.
 *
 * It also makes sure that the triggering and securing method call is made on the same symbol.
 *
 */
public abstract class TriggeringSecuringHelper implements Predicate<MethodInvocationTree> {

  private final MethodMatcher triggeringInvocationMatcher;

  /**
   * Called before each visit of the enclosing method body, in {@link #test(MethodInvocationTree)}.
   * Typically used to reset the different variable used during {@link #processSecuringMethodInvocation(MethodInvocationTree)}.
   */
  public abstract void resetState();

  /**
   * The method visitor call this method for all method invocation in the method containing the triggering call.
   * It typically contributes to define if the triggering call is secured or not.
   * (see {@link #isSecured()})
   */
  public abstract void processSecuringMethodInvocation(MethodInvocationTree mit);

  /**
   * At the end of the visit of the enclosing method body, return true if all expected securing method call were found.
   */
  public abstract boolean isSecured();

  TriggeringSecuringHelper(MethodMatcher triggeringInvocationMatcher) {
    this.triggeringInvocationMatcher = triggeringInvocationMatcher;
  }

  @Override
  public boolean test(MethodInvocationTree mit) {
    if (triggeringInvocationMatcher.matches(mit)) {
      MethodTree enclosingMethod = ExpressionUtils.getEnclosingMethod(mit);
      if (enclosingMethod != null) {
        Optional<Symbol> assignedSymbol = getAssignedSymbol(mit);
        if (assignedSymbol.isPresent()) {
          resetState();
          enclosingMethod.accept(new MethodVisitor(assignedSymbol.get()));
          return !isSecured();
        }
      }
    }
    return false;
  }

  static boolean isInvocationOnVariable(MethodInvocationTree mit, Symbol variable) {
    ExpressionTree methodSelect = mit.methodSelect();
    return methodSelect.is(Tree.Kind.MEMBER_SELECT)
      && extractIdentifierSymbol(((MemberSelectExpressionTree) methodSelect).expression()).filter(s -> s.equals(variable)).isPresent();
  }

  private class MethodVisitor extends BaseTreeVisitor {

    private Symbol variable;

    private MethodVisitor(Symbol variable) {
      this.variable = variable;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (isInvocationOnVariable(methodInvocation, variable)) {
        processSecuringMethodInvocation(methodInvocation);
      }
      super.visitMethodInvocation(methodInvocation);
    }
  }
}
