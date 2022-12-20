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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

@Rule(key = "S1181")
public class CatchOfThrowableOrErrorCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";
  private final TryBlockVisitor tryBlockVisitor = new TryBlockVisitor();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tryStatement = (TryStatementTree) tree;
    tryStatement.block().accept(tryBlockVisitor);
    if (tryBlockVisitor.containsExplicitThrowable || tryBlockVisitor.containsUnresolvableCall) {
      return;
    }
    for (CatchTree catchTree : tryStatement.catches()) {
      TypeTree typeTree = catchTree.parameter().type();
      if (typeTree.is(Tree.Kind.UNION_TYPE)) {
        for (TypeTree alternativeTypeTree : ((UnionTypeTree) typeTree).typeAlternatives()) {
          checkType(alternativeTypeTree, catchTree);
        }
      } else {
        checkType(typeTree, catchTree);
      }
    }
  }

  private void checkType(TypeTree typeTree, CatchTree catchTree) {
    Type type = typeTree.symbolType();
    if (type.is("java.lang.Error")) {
      insertIssue(typeTree, type);
    } else if (type.is(JAVA_LANG_THROWABLE)) {
      ReThrowVisitor visitor = new ReThrowVisitor(catchTree.parameter().symbol());
      catchTree.block().accept(visitor);
      if (!visitor.foundRethrow) {
        insertIssue(typeTree, type);
      }
    }
  }

  private void insertIssue(TypeTree typeTree, Type type) {
    reportIssue(typeTree, "Catch Exception instead of " + type.name() + ".");
  }

  private static class ReThrowVisitor extends BaseTreeVisitor {
    private static final String JAVA_LANG_CLASS = "java.lang.Class";
    private static final MethodMatchers MATCHERS = MethodMatchers.create()
      .ofTypes("com.google.common.io.Closer")
      .names("rethrow")
      .addParametersMatcher(JAVA_LANG_THROWABLE)
      .addParametersMatcher(JAVA_LANG_THROWABLE, JAVA_LANG_CLASS)
      .addParametersMatcher(JAVA_LANG_THROWABLE, JAVA_LANG_CLASS, JAVA_LANG_CLASS)
      .build();

    private boolean foundRethrow = false;
    private final Symbol exceptionSymbol;

    public ReThrowVisitor(Symbol exceptionSymbol) {
      this.exceptionSymbol = exceptionSymbol;
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      ExpressionTree expression = tree.expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) expression;
        // In case of broken semantics, the method matcher cannot identify the method invocation.
        // To avoid raising FPs in these cases, we accept it as is and move on.
        if (mit.methodSymbol().isUnknown() || isGuavaCloserRethrow(mit)) {
          foundRethrow = true;
        }
      }
    }

    private boolean isGuavaCloserRethrow(MethodInvocationTree mit) {
      if (!MATCHERS.matches(mit)) {
        return false;
      }
      ExpressionTree firstArgument = mit.arguments().get(0);
      return firstArgument.is(Tree.Kind.IDENTIFIER) && exceptionSymbol.equals(((IdentifierTree) firstArgument).symbol());
    }
  }

  private static class TryBlockVisitor extends BaseTreeVisitor {
    private boolean containsExplicitThrowable;
    private boolean containsUnresolvableCall;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      checkIfThrowThrowable(tree.methodSymbol());
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      checkIfThrowThrowable(tree.methodSymbol());
      super.visitNewClass(tree);
    }

    private void checkIfThrowThrowable(Symbol.MethodSymbol symbol) {
      if (containsExplicitThrowable || containsUnresolvableCall) {
        return;
      }
      if (symbol.isUnknown()) {
        containsUnresolvableCall = true;
        return;
      }
      for (Type type : symbol.thrownTypes()) {
        if (type.is(JAVA_LANG_THROWABLE)) {
          containsExplicitThrowable = true;
          return;
        }
      }
    }
  }
}
