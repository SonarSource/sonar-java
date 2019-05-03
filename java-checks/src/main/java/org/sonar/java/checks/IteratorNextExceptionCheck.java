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
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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

  private static final MethodMatcher NEXT_INVOCATION_MATCHER =
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Iterator"))
      .name("next")
      .withoutParameter();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && isIteratorNextMethod(methodTree.symbol()) && methodTree.block() != null) {
      NextMethodBodyVisitor visitor = new NextMethodBodyVisitor();
      tree.accept(visitor);
      if (!visitor.foundThrow) {
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

    private boolean foundThrow = false;

    @Override
    public void visitThrowStatement(ThrowStatementTree throwStatementTree) {
      ExpressionTree expression = throwStatementTree.expression();
      if (expression.is(Tree.Kind.NEW_CLASS)) {
        NewClassTree newClassTree = (NewClassTree) expression;
        if (newClassTree.symbolType().is("java.util.NoSuchElementException")) {
          foundThrow = true;
        }
      }
      super.visitThrowStatement(throwStatementTree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (NEXT_INVOCATION_MATCHER.matches(methodInvocation) || throwsNoSuchElementException(methodInvocation)) {
        foundThrow = true;
      }
      super.visitMethodInvocation(methodInvocation);
    }

    private static boolean throwsNoSuchElementException(MethodInvocationTree methodInvocationTree) {
      Symbol symbol = methodInvocationTree.symbol();
      if (!symbol.isMethodSymbol()) {
        return false;
      }
      if (throwsNoSuchElementException(((Symbol.MethodSymbol) symbol).thrownTypes())) {
        return true;
      }
      MethodJavaType methodJavaType = (MethodJavaType) ExpressionUtils.methodName(methodInvocationTree).symbolType();
      return throwsNoSuchElementException(methodJavaType.thrownTypes());
    }

    private static boolean throwsNoSuchElementException(List<? extends Type> thrownTypes) {
      return thrownTypes.stream().anyMatch(t -> t.is("java.util.NoSuchElementException"));
    }

  }

}
