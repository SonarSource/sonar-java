/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.matcher;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.matcher.TreeMatcher.any;
import static org.sonar.java.matcher.TreeMatcher.forEachStatement;
import static org.sonar.java.matcher.TreeMatcher.hasSize;
import static org.sonar.java.matcher.TreeMatcher.invokedOn;
import static org.sonar.java.matcher.TreeMatcher.isCall;
import static org.sonar.java.matcher.TreeMatcher.isExpression;
import static org.sonar.java.matcher.TreeMatcher.isIdentifier;
import static org.sonar.java.matcher.TreeMatcher.isInvocation;
import static org.sonar.java.matcher.TreeMatcher.matching;
import static org.sonar.java.matcher.TreeMatcher.statementAt;
import static org.sonar.java.matcher.TreeMatcher.withArgument;
import static org.sonar.java.matcher.TreeMatcher.withBody;
import static org.sonar.java.matcher.TreeMatcher.withExpression;

class TreeMatcherTest {

  @Test
  void isLambdaExpression() {
    CompilationUnitTree t = JParserTestUtils.parse("""
       class C {
         Consumer<String> lambda = (String s) -> {
           System.out.println(s);
         };
         Consumer<String> lambda2 = (String s) -> foo(s);
         Consumer<List<String>> lambda3 = s -> { for (String s1 : s) {
           System.out.println(s);
         }};
         void foo(String s) {}
       }
      """);

    ClassTree c = (ClassTree) t.types().get(0);

    VariableTree lambdaAssignment = (VariableTree) c.members().get(0);
    ExpressionTree lambda = lambdaAssignment.initializer();

    VariableTree paramSymbol = ((LambdaExpressionTree) lambda).parameters().get(0);
    TreeMatcher<ExpressionTree> lambdaWithStatementsMatcher = TreeMatcher.isLambdaExpression(withBody(hasSize(1).and(
      statementAt(0,
        isInvocation(withArgument(0, isIdentifier("s")))))));
    assertTrue(
      lambdaWithStatementsMatcher
        .check(lambda));
    assertTrue(
      TreeMatcher.isLambdaExpression(withBody(hasSize(1).and(
        statementAt(0,
          isInvocation(withArgument(0, isIdentifier(paramSymbol.symbol())))))))
        .check(lambda));
    assertFalse(TreeMatcher.isIdentifier("foo").check(lambda));

    VariableTree lambda2Assignment = (VariableTree) c.members().get(1);
    ExpressionTree lambda2 = lambda2Assignment.initializer();
    assertTrue(
      TreeMatcher.isLambdaExpression(withBody(isExpression(
        isCall(withArgument(0, isIdentifier("s"))))))
        .check(lambda2));

    VariableTree lambda3Assignment = (VariableTree) c.members().get(2);
    TreeMatcher<ExpressionTree> forEachInLambdaMatcher = TreeMatcher.isLambdaExpression(
      withBody(statementAt(0, forEachStatement(withExpression(isIdentifier("s"))))));
    ExpressionTree lambda3 = lambda3Assignment.initializer();
    assertTrue(forEachInLambdaMatcher.check(lambda3));

    assertFalse(lambdaWithStatementsMatcher.check(lambda3));
    assertTrue(lambdaWithStatementsMatcher.or(forEachInLambdaMatcher).check(lambda3));
    assertFalse(forEachInLambdaMatcher.check(lambda));
    assertFalse(forEachInLambdaMatcher.check(lambda2));
  }

  @Test
  void testAny() {
    CompilationUnitTree t = JParserTestUtils.parse("""
            class C {
              int i = foo(x);
            }
            """);

    ClassTree c = (ClassTree) t.types().get(0);
    VariableTree variableTree = (VariableTree) c.members().get(0);
    ExpressionTree expression = variableTree.initializer();

    assertTrue(any().check(c));
    assertTrue(any().check(variableTree));
    assertTrue(any().check(expression));
  }

  @Test
  void testAsPredicate() {
    CompilationUnitTree t = JParserTestUtils.parse("""
      class C {
        int i = foo(x);
      }
      """);

    ClassTree c = (ClassTree) t.types().get(0);
    VariableTree variableTree = (VariableTree) c.members().get(0);
    ExpressionTree expression = variableTree.initializer();

    TreeMatcher<Tree> matcher = isExpression(isCall(withArgument(0, isIdentifier("x"))));
    Predicate<Tree> predicate = matcher.asPredicate();
    assertTrue(predicate.test(expression));
  }

  @Test
  void testRecursive() {
    CompilationUnitTree t = JParserTestUtils.parse("""
      class C {
        int i = foo(x);
      }
      """);

    // Match any number of calls to foo on itself or x
    TreeMatcher<ExpressionTree> fooStarX = TreeMatcher
      .recursive(self -> isIdentifier("x").or(isCall(withArgument(0, self))));

    ClassTree c = (ClassTree) t.types().get(0);
    VariableTree variableTree = (VariableTree) c.members().get(0);
    ExpressionTree expression = variableTree.initializer();
    assertTrue(fooStarX.check(expression));
  }

  @Test
  void testIsIdentifier() {
    CompilationUnitTree t = JParserTestUtils.parse("""
      class C {
        int i = x;
        int j = foo(x);
      }
      """);

    ClassTree c = (ClassTree) t.types().get(0);
    ExpressionTree xInI = ((VariableTree) c.members().get(0)).initializer();
    ExpressionTree fooX = ((VariableTree) c.members().get(1)).initializer();
    ExpressionTree xInJ = ((MethodInvocationTree) fooX).arguments().get(0);
    Symbol i = ((VariableTree) c.members().get(0)).symbol();
    assertTrue(isIdentifier("x").check(xInI));
    assertFalse(isIdentifier("y").check(xInI));
    assertTrue(isIdentifier((IdentifierTree) xInI).check(xInJ));
    assertFalse(isIdentifier(i).check(xInI));
    assertFalse(isIdentifier(i).check(fooX));

    assertFalse(TreeMatcher
      .isLambdaExpression(matching(tree -> true))
      .check(xInI));
    assertFalse(isExpression(matching(e -> true)).check(c));
    assertFalse(isExpression(isIdentifier("y")).check(xInI));
  }

  @Test
  void testInvokedOn() {
    CompilationUnitTree t = JParserTestUtils.parse("""
      class C {
        int i = x.foo(t);
      }
      """);

    ClassTree c = (ClassTree) t.types().get(0);
    ExpressionTree callOnX = ((VariableTree) c.members().get(0)).initializer();
    assertTrue(isCall(invokedOn(isIdentifier("x"))).check(callOnX));
    assertFalse(isCall(invokedOn(isIdentifier("y"))).check(callOnX));

    assertTrue(isCall(withArgument(0, isIdentifier("t"))).check(callOnX));
    assertFalse(isCall(withArgument(1, isIdentifier("t"))).check(callOnX));
    assertFalse(isCall(withArgument(0, isIdentifier("u"))).check(callOnX));
  }

  @Test
  void testCalls() {
    CompilationUnitTree t = JParserTestUtils.parse("""
      class C {
        int i = x.bar();
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    ExpressionTree callOnX = ((VariableTree) c.members().get(0)).initializer();

    MethodMatchers matchAll = mock(MethodMatchers.class);
    when(matchAll.matches(ArgumentMatchers.any(MethodInvocationTree.class))).thenReturn(true);

    MethodMatchers matchNone = mock(MethodMatchers.class);
    when(matchNone.matches(ArgumentMatchers.any(MethodInvocationTree.class))).thenReturn(false);

    assertTrue(TreeMatcher.calls(matchAll, invokedOn(isIdentifier("x"))).check(callOnX));
    assertFalse(TreeMatcher.calls(matchAll, invokedOn(isIdentifier("y"))).check(callOnX));
    assertFalse(TreeMatcher.calls(matchNone, invokedOn(isIdentifier("x"))).check(callOnX));
  }

  @Test
  void testIsInvocationOf() {
    CompilationUnitTree t = JParserTestUtils.parse("""
      class C {
        Supplier<String> s = () -> { x.bar(); return y.baz(); };
      }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    ExpressionTree lambda = ((VariableTree) c.members().get(0)).initializer();
    StatementTree firstStatement = ((BlockTree) ((LambdaExpressionTree) lambda).body()).body().get(0);

    MethodMatchers matchAll = mock(MethodMatchers.class);
    when(matchAll.matches(ArgumentMatchers.any(MethodInvocationTree.class))).thenReturn(true);

    MethodMatchers matchNone = mock(MethodMatchers.class);
    when(matchNone.matches(ArgumentMatchers.any(MethodInvocationTree.class))).thenReturn(false);

    assertTrue(TreeMatcher.isInvocationOf(matchAll, invokedOn(isIdentifier("x"))).check(firstStatement));
    assertFalse(TreeMatcher.isInvocationOf(matchAll, invokedOn(isIdentifier("y"))).check(firstStatement));
    assertFalse(TreeMatcher.isInvocationOf(matchNone, invokedOn(isIdentifier("x"))).check(firstStatement));
  }

  @Test
  void testHasSize() {
    CompilationUnitTree t = JParserTestUtils.parse("""
       class C {
         Consumer<List<String>> lambda = s -> {
           int i = 0;
           for (String s1 : s) {
             i++;
           }
           return i;
         };
       }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    ExpressionTree lambda = ((VariableTree) c.members().get(0)).initializer();
    BlockTree block = (BlockTree) ((LambdaExpressionTree) lambda).body();

    assertTrue(hasSize(3).check(block));
    assertFalse(hasSize(2).check(block));
    assertTrue(hasSize(1).check(block.body().get(0)));
    assertFalse(hasSize(2).check(block.body().get(0)));

    // Expressions are not blocks, so they cannot have a size
    assertFalse(hasSize(1).check(lambda));
  }

  @Test
  void testStatementAt() {
    CompilationUnitTree t = JParserTestUtils.parse("""
       class C {
         Consumer<Integer> lambda = x -> {
           int i = x;
           int j = x;
           k = i + j;
         };
       }
      """);
    ClassTree c = (ClassTree) t.types().get(0);
    ExpressionTree lambda = ((VariableTree) c.members().get(0)).initializer();
    BlockTree block = (BlockTree) ((LambdaExpressionTree) lambda).body();

    assertTrue(statementAt(0, matching(VariableTree.class::isInstance)).check(block));
    assertFalse(statementAt(0, isInvocation(matching(s -> true)))
      .check(block));
    assertFalse(statementAt(10, isInvocation(matching(s -> true)))
      .check(block));

    assertTrue(statementAt(0, matching(ExpressionStatementTree.class::isInstance)).check(block.body().get(2)));
    assertFalse(statementAt(2, matching(ExpressionStatementTree.class::isInstance)).check(block.body().get(2)));
    assertFalse(statementAt(0, matching(VariableTree.class::isInstance)).check(block.body().get(2)));

  }

}
