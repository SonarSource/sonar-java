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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2272")
public class IteratorNextExceptionCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers NEXT_INVOCATION_MATCHER = MethodMatchers.create()
      .ofSubTypes("java.util.Iterator")
      .name(name -> name.startsWith("next") || name.startsWith("previous"))
      .addWithoutParametersMatcher()
      .build();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isIteratorNextMethod(methodTree.symbol()) && methodTree.block() != null) {
      NextMethodBodyVisitor visitor = new NextMethodBodyVisitor();
      visitor.methodsVisited.add(methodTree);
      tree.accept(visitor);
      if (!visitor.expectedExceptionIsThrown) {
        reportIssue(methodTree.simpleName(), "Add a \"NoSuchElementException\" for iteration beyond the end of the collection.");
      }
    }
  }

  private static boolean isIteratorNextMethod(Symbol.MethodSymbol symbol) {
    return "next".equals(symbol.name()) && symbol.parameterTypes().isEmpty() && isIterator(symbol.enclosingClass());
  }

  private static boolean isIterator(Symbol.TypeSymbol typeSymbol) {
    return typeSymbol.type().isSubtypeOf("java.util.Iterator");
  }

  private static class NextMethodBodyVisitor extends BaseTreeVisitor {
    private boolean expectedExceptionIsThrown = false;
    private final Set<MethodTree> methodsVisited = new HashSet<>();

    @Override
    public void visitThrowStatement(ThrowStatementTree throwStatementTree) {
      ExpressionTree expression = throwStatementTree.expression();
      if (expression.is(Tree.Kind.NEW_CLASS)) {
        NewClassTree newClassTree = (NewClassTree) expression;
        Type symbolType = newClassTree.symbolType();
        if (symbolType.isSubtypeOf("java.util.NoSuchElementException") || symbolType.isUnknown()) {
          // Consider any unknown Exception as NoSuchElementException to avoid FP.
          expectedExceptionIsThrown = true;
        }
      }
      super.visitThrowStatement(throwStatementTree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (NEXT_INVOCATION_MATCHER.matches(methodInvocation) || throwsNoSuchElementException(methodInvocation)) {
        expectedExceptionIsThrown = true;
      } else if (methodInvocation.symbol().isMethodSymbol()) {
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) methodInvocation.symbol();
        MethodTree methodTree = methodSymbol.declaration();
        boolean canVisit = methodTree != null && methodsVisited.add(methodTree);
        if (canVisit) {
          scan(methodTree);
        }
      }
      super.visitMethodInvocation(methodInvocation);
    }

    private static boolean throwsNoSuchElementException(MethodInvocationTree methodInvocationTree) {
      Symbol.MethodSymbol symbol = methodInvocationTree.methodSymbol();
      if (symbol.isUnknown()) {
        // Consider that it could throw an Exception to avoid FP.
        return true;
      }
      return throwsNoSuchElementException(symbol.thrownTypes());
    }

    private static boolean throwsNoSuchElementException(List<? extends Type> thrownTypes) {
      return thrownTypes.stream().anyMatch(t -> t.isSubtypeOf("java.util.NoSuchElementException") || t.isUnknown());
    }

  }

}
