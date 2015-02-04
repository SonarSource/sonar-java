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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type.ClassType;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2272",
  name = "\"Iterator.next()\" methods should throw \"NoSuchElementException\"",
  tags = {"bug"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("5min")
public class IteratorNextExceptionCheck extends SubscriptionBaseVisitor {

  private static final MethodInvocationMatcher NEXT_INVOCATION_MATCHER =
    MethodInvocationMatcher.create()
      .typeDefinition("java.util.Iterator")
      .name("next");

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTreeImpl methodTree = (MethodTreeImpl) tree;
    if (hasSemantic() && isIteratorNextMethod(methodTree.getSymbol()) && methodTree.block() != null) {
      NextMethodBodyVisitor visitor = new NextMethodBodyVisitor();
      tree.accept(visitor);
      if (!visitor.foundThrow) {
        addIssue(tree, "Add a \"NoSuchElementException\" for iteration beyond the end of the collection.");
      }
    }
  }

  private boolean isIteratorNextMethod(MethodSymbol symbol) {
    return "next".equals(symbol.getName()) && symbol.getParametersTypes().isEmpty() && isIterator(symbol.enclosingClass());
  }

  private boolean isIterator(TypeSymbol typeSymbol) {
    for (ClassType superType : typeSymbol.superTypes()) {
      if (superType.is("java.util.Iterator")) {
        return true;
      }
    }
    return false;
  }

  private class NextMethodBodyVisitor extends BaseTreeVisitor {

    private boolean foundThrow = false;

    @Override
    public void visitThrowStatement(ThrowStatementTree throwStatementTree) {
      ExpressionTree expression = throwStatementTree.expression();
      if (expression.is(Tree.Kind.NEW_CLASS)) {
        NewClassTreeImpl newClassTree = (NewClassTreeImpl) expression;
        if (newClassTree.getSymbolType().is("java.util.NoSuchElementException")) {
          foundThrow = true;
        }
      }
      super.visitThrowStatement(throwStatementTree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (NEXT_INVOCATION_MATCHER.matches(methodInvocation, getSemanticModel()) || throwsNoSuchElementException(methodInvocation)) {
        foundThrow = true;
      }
      super.visitMethodInvocation(methodInvocation);
    }

    public boolean throwsNoSuchElementException(MethodInvocationTree methodInvocationTree) {
      MethodInvocationTreeImpl methodInvocationTreeImpl = (MethodInvocationTreeImpl) methodInvocationTree;
      Symbol symbol = methodInvocationTreeImpl.getSymbol();
      if (!symbol.isKind(Symbol.MTH)) {
        return false;
      }
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      if (methodSymbol.getThrownTypes() != null) {
        for (TypeSymbol thrownType : methodSymbol.getThrownTypes()) {
          if (thrownType.getType().is("java.util.NoSuchElementException")) {
            return true;
          }
        }
      }
      return false;
    }

  }

}
