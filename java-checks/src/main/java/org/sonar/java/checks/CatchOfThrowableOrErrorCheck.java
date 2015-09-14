/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1181",
  name = "Throwable and Error should not be caught",
  tags = {"cert", "cwe", "error-handling", "security"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.EXCEPTION_HANDLING)
@SqaleConstantRemediation("20min")
public class CatchOfThrowableOrErrorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    CatchTree catchTree = (CatchTree) tree;
    TypeTree typeTree = catchTree.parameter().type();

    if (typeTree.is(Tree.Kind.UNION_TYPE)) {
      for (TypeTree alternativeTypeTree : ((UnionTypeTree) typeTree).typeAlternatives()) {
        checkType(alternativeTypeTree, catchTree);
      }
    } else {
      checkType(typeTree, catchTree);
    }
  }

  private void checkType(TypeTree typeTree, CatchTree catchTree) {
    Type type = typeTree.symbolType();
    if (type.is("java.lang.Error")) {
      insertIssue(typeTree, type);
    } else if (type.is("java.lang.Throwable")) {
      GuavaCloserRethrowVisitor visitor = new GuavaCloserRethrowVisitor(catchTree.parameter().symbol());
      catchTree.block().accept(visitor);
      if (!visitor.foundRethrow) {
        insertIssue(typeTree, type);
      }
    }
  }

  private void insertIssue(TypeTree typeTree, Type type) {
    addIssue(typeTree, "Catch Exception instead of " + type.name() + ".");
  }

  private static class GuavaCloserRethrowVisitor extends BaseTreeVisitor {
    private static final String JAVA_LANG_CLASS = "java.lang.Class";
    private static final MethodInvocationMatcherCollection MATCHERS = MethodInvocationMatcherCollection.create(
      rethrowMethod(),
      rethrowMethod().addParameter(JAVA_LANG_CLASS),
      rethrowMethod().addParameter(JAVA_LANG_CLASS).addParameter(JAVA_LANG_CLASS));

    private boolean foundRethrow = false;
    private final Symbol exceptionSymbol;

    public GuavaCloserRethrowVisitor(Symbol exceptionSymbol) {
      this.exceptionSymbol = exceptionSymbol;
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      if (isGuavaCloserRethrow(tree.expression())) {
        foundRethrow = true;
      }
    }

    private boolean isGuavaCloserRethrow(ExpressionTree expression) {
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) expression;
        if (MATCHERS.anyMatch(mit)) {
          ExpressionTree firstArgument = mit.arguments().get(0);
          return firstArgument.is(Tree.Kind.IDENTIFIER) && exceptionSymbol.equals(((IdentifierTree) firstArgument).symbol());
        }
      }
      return false;
    }

    private static MethodMatcher rethrowMethod() {
      return MethodMatcher.create().typeDefinition("com.google.common.io.Closer").name("rethrow").addParameter("java.lang.Throwable");
    }
  }
}
