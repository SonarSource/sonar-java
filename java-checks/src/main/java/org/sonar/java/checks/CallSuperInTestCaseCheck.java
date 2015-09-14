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
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

@Rule(
  key = "S2188",
  name = "JUnit test cases should call super methods",
  tags = {"bug", "junit"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class CallSuperInTestCaseCheck extends SubscriptionBaseVisitor {

  public static final String JUNIT_FRAMEWORK_TEST_CASE = "junit.framework.TestCase";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    boolean isMethodInJunit3 = isWithinJunit3TestCase(methodSymbol) && isSetUpOrTearDown(methodSymbol);
    if (isMethodInJunit3 && requiresSuperCall(methodSymbol) && !callSuperOnOverride(methodTree.block(), methodSymbol)) {
      addIssue(tree, String.format("Add a \"super.%s()\" call to this method.", methodSymbol.name()));
    }
  }

  private static boolean requiresSuperCall(Symbol.MethodSymbol methodSymbol) {
    Type superType = methodSymbol.owner().type().symbol().superClass();
    Collection<Symbol> symbols = Lists.newArrayList();
    while (superType != null && !superType.is(JUNIT_FRAMEWORK_TEST_CASE) && symbols.isEmpty()) {
      symbols = superType.symbol().lookupSymbols(methodSymbol.name());
      superType = superType.symbol().superClass();
    }
    return !symbols.isEmpty() && !symbols.iterator().next().owner().type().is(JUNIT_FRAMEWORK_TEST_CASE);
  }

  private static boolean callSuperOnOverride(@Nullable BlockTree block, Symbol.MethodSymbol methodSymbol) {
    if (block == null) {
      return false;
    }
    InvocationVisitor visitor = new InvocationVisitor(methodSymbol.name());
    block.accept(visitor);
    return visitor.superCallOnOverride;
  }

  private static boolean isWithinJunit3TestCase(Symbol.MethodSymbol methodSymbol) {
    Type type = methodSymbol.owner().type();
    return type.isSubtypeOf(JUNIT_FRAMEWORK_TEST_CASE) && !type.symbol().superClass().is(JUNIT_FRAMEWORK_TEST_CASE);
  }

  private static boolean isSetUpOrTearDown(Symbol.MethodSymbol methodSymbol) {
    return ("setUp".equals(methodSymbol.name()) || "tearDown".equals(methodSymbol.name()))
      && methodSymbol.parameterTypes().isEmpty();
  }

  private static class InvocationVisitor extends BaseTreeVisitor {

    private final String methodName;
    private boolean superCallOnOverride = false;

    InvocationVisitor(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree.methodSelect();
        if (mse.expression().is(Tree.Kind.IDENTIFIER) && "super".equals(((IdentifierTree) mse.expression()).name()) && mse.identifier().name().equals(methodName)) {
          superCallOnOverride |= !((IdentifierTree) mse.expression()).symbol().type().is(JUNIT_FRAMEWORK_TEST_CASE);
        }
      }
      super.visitMethodInvocation(tree);
    }

  }
}
