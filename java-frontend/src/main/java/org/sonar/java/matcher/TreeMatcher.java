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

import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class TreeMatcher<T extends Tree> {
  private final Predicate<T> predicate;

  private TreeMatcher(Predicate<T> predicate) {
    this.predicate = predicate;
  }

  public boolean check(T tree) {
    return predicate.test(tree);
  }

  public TreeMatcher<T> and(TreeMatcher<T> other) {
    return new TreeMatcher<>(predicate.and(other.predicate));
  }

  public TreeMatcher<T> or(TreeMatcher<T> other) {
    return new TreeMatcher<>(predicate.or(other.predicate));
  }

  /** Method to allow defining a matcher that refers to itself. */
  public static <T extends Tree> TreeMatcher<T> recursive(UnaryOperator<TreeMatcher<T>> treeMatcherMaker) {
    AtomicReference<TreeMatcher<T>> treeMatcherWrapper = new AtomicReference<>();
    TreeMatcher<T> matcherUsingWrapper = new TreeMatcher<>(tree -> treeMatcherWrapper.get().check(tree));
    TreeMatcher<T> matcher = treeMatcherMaker.apply(matcherUsingWrapper);
    treeMatcherWrapper.set(matcher);
    return treeMatcherWrapper.get();
  }

  public static <U extends Tree> TreeMatcher<U> matching(Predicate<U> predicate) {
    return new TreeMatcher<>(predicate);
  }

  public static <U extends Tree> TreeMatcher<U> any() {
    return new TreeMatcher<>(x -> true);
  }

  public Predicate<T> asPredicate() {
    return predicate;
  }

  public static TreeMatcher<ExpressionTree> calls(
    MethodMatchers methodMatchers, TreeMatcher<MethodInvocationTree> methodInvocationMatcher) {
    return new TreeMatcher<>(
      expressionTree -> (expressionTree instanceof MethodInvocationTree mit
        && methodMatchers.matches(mit) && methodInvocationMatcher.check(mit)));
  }

  public static TreeMatcher<ExpressionTree> calls(MethodMatchers methodMatchers) {
    return calls(methodMatchers, any());
  }

  public static TreeMatcher<ExpressionTree> isCall(TreeMatcher<MethodInvocationTree> methodInvocationMatcher) {
    return new TreeMatcher<>(
      expressionTree -> (expressionTree instanceof MethodInvocationTree mit
        && methodInvocationMatcher.check(mit)));
  }

  public static TreeMatcher<StatementTree> isInvocationOf(
    MethodMatchers methodMatchers, TreeMatcher<MethodInvocationTree> methodInvocationMatcher) {
    return new TreeMatcher<>(
      statementTree -> (statementTree instanceof ExpressionStatementTree exprStatement
        && calls(methodMatchers, methodInvocationMatcher).check(exprStatement.expression())));
  }

  public static TreeMatcher<StatementTree> isInvocationOf(MethodMatchers methodMatchers) {
    return isInvocationOf(methodMatchers, any());
  }

  public static TreeMatcher<StatementTree> isInvocation(TreeMatcher<MethodInvocationTree> methodInvocationMatcher) {
    return new TreeMatcher<>(
      statementTree -> (statementTree instanceof ExpressionStatementTree exprStatement
        && isCall(methodInvocationMatcher).check(exprStatement.expression())));
  }

  public static TreeMatcher<ExpressionTree> isIdentifier(String name) {
    return new TreeMatcher<>(expressionTree -> expressionTree instanceof IdentifierTree id && id.name().equals(name));
  }

  public static TreeMatcher<ExpressionTree> isIdentifier(Symbol symbol) {
    return new TreeMatcher<>(expressionTree -> expressionTree instanceof IdentifierTree id && id.symbol().equals(symbol));
  }

  public static TreeMatcher<ExpressionTree> isIdentifier(IdentifierTree identifier) {
    return isIdentifier(identifier.symbol());
  }

  public static TreeMatcher<ExpressionTree> isLambdaExpression(TreeMatcher<LambdaExpressionTree> lambdaMatcher) {
    return new TreeMatcher<>(expressionTree -> expressionTree instanceof LambdaExpressionTree lambda && lambdaMatcher.check(lambda));
  }

  public static TreeMatcher<LambdaExpressionTree> withBody(TreeMatcher<Tree> bodyMatcher) {
    return new TreeMatcher<>(lambda -> bodyMatcher.check(lambda.body()));
  }

  public static TreeMatcher<Tree> isExpression(TreeMatcher<ExpressionTree> expressionMatcher) {
    return new TreeMatcher<>(tree -> tree instanceof ExpressionTree expr && expressionMatcher.check(expr));
  }

  public static TreeMatcher<Tree> hasSize(int size) {
    return new TreeMatcher<>(tree -> (tree instanceof BlockTree block && block.body().size() == size)
      || (tree instanceof StatementTree && size == 1));
  }

  public static TreeMatcher<Tree> statementAt(int index, TreeMatcher<StatementTree> statementMatcher) {
    return new TreeMatcher<>(tree ->
      ((tree instanceof BlockTree block && block.body().size() > index && statementMatcher.check(block.body().get(index)))
      || (tree instanceof ExpressionStatementTree statement && index == 0 && statementMatcher.check(statement))));
  }

  public static TreeMatcher<StatementTree> forEachStatement(TreeMatcher<ForEachStatement> forEachMatcher) {
    return new TreeMatcher<>(statement -> statement instanceof ForEachStatement forEachStatement &&
      forEachMatcher.check(forEachStatement));
  }

  public static TreeMatcher<ForEachStatement> withExpression(TreeMatcher<ExpressionTree> expressionMatcher) {
    return new TreeMatcher<>(forEach -> expressionMatcher.check(forEach.expression()));
  }

  public static TreeMatcher<MethodInvocationTree> invokedOn(TreeMatcher<ExpressionTree> expressionMatcher) {
    return new TreeMatcher<>(mit -> mit.methodSelect() instanceof MemberSelectExpressionTree mset
      && expressionMatcher.check(mset.expression()));
  }

  public static TreeMatcher<MethodInvocationTree> withArgument(int index, TreeMatcher<ExpressionTree> expressionMatcher) {
    return new TreeMatcher<>(mit -> mit.arguments().size() > index && expressionMatcher.check(mit.arguments().get(index)));
  }
}
