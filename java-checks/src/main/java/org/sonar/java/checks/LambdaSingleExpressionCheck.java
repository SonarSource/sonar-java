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
package org.sonar.java.checks;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1602")
public class LambdaSingleExpressionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) tree;
    Tree lambdaBody = lambdaExpressionTree.body();
    if (isBlockWithOneStatement(lambdaBody)) {
      String message = "Remove useless curly braces around statement";
      if (singleStatementIsReturn(lambdaExpressionTree)) {
        message += " and then remove useless return keyword";
      } else if(locationHasAmbiguousType(tree)) {
        return;
      }
      reportIssue(((BlockTree) lambdaBody).openBraceToken(), message + context.getJavaVersion().java8CompatibilityMessage());
    }
  }

  private static boolean isBlockWithOneStatement(Tree tree) {
    boolean result = false;
    if (tree.is(Tree.Kind.BLOCK)) {
      List<StatementTree> blockBody = ((BlockTree) tree).body();
      result = blockBody.size() == 1 && isRefactorizable(blockBody.get(0));
    }
    return result;
  }

  private static boolean isRefactorizable(StatementTree statementTree) {
    return isBlockWithOneStatement(statementTree) || statementTree.is(Tree.Kind.EXPRESSION_STATEMENT) || isReturnStatement(statementTree);
  }

  private static boolean singleStatementIsReturn(LambdaExpressionTree lambdaExpressionTree) {
    return isReturnStatement(((BlockTree) lambdaExpressionTree.body()).body().get(0));
  }

  private static boolean isReturnStatement(Tree tree) {
    return tree.is(Tree.Kind.RETURN_STATEMENT);
  }

  record InvocationData(Symbol.MethodSymbol methodSymbol, int argumentPosition) {}

  /**
   * Assuming {@code tree} is an argument in a method invocation,
   * get the symbol of the method and the position of the argument.
   */
  private static Optional<InvocationData> getInvocationData(Tree argument) {
    Optional<Arguments> argumentList = Optional.ofNullable(argument.parent())
      .filter(Arguments.class::isInstance)
      .map(Arguments.class::cast);

    if(argumentList.isPresent()) {
      int position = argumentList.get().indexOf(argument);
      return argumentList
        .map(Tree::parent)
        .filter(MethodInvocationTree.class::isInstance)
        .map(MethodInvocationTree.class::cast)
        .map(mit -> new InvocationData(mit.methodSymbol(), position));
    }

    return Optional.empty();
  }

  /**
   * Get all overloaded versions of the argument.
   */
  private static Collection<Symbol> getOverloads(Symbol.MethodSymbol methodSymbol) {
    Symbol.TypeSymbol owner = (Symbol.TypeSymbol) methodSymbol.owner();
    return owner.lookupSymbols(methodSymbol.name());
  }

  /**
   * Given a collection of method symbols and a number indicating position
   * inside a parameter list, get the distinct types possible at this position.
   */
  private static Stream<Type> getTypesAtPosition(Collection<Symbol> symbols, int position) {
    return symbols.stream()
      .filter(Symbol.MethodSymbol.class::isInstance)
      .map(Symbol.MethodSymbol.class::cast)
      .map(Symbol.MethodSymbol::parameterTypes)
      .flatMap(parameterTypes ->
        position < parameterTypes.size()
          ? Stream.of(parameterTypes.get(position))
          : Stream.empty()
      )
      .distinct();
  }

  /**
   * If the tree is an argument in a call to an overloaded method,
   * then check if it is in a position where multiple types are possible.
   */
  private static boolean locationHasAmbiguousType(Tree tree) {
    Optional<InvocationData> invocationData = getInvocationData(tree);

    if (invocationData.isPresent()) {
      Symbol.MethodSymbol methodSymbol = invocationData.get().methodSymbol();
      Collection<Symbol> overloads = getOverloads(methodSymbol);
      return getTypesAtPosition(overloads, invocationData.get().argumentPosition()).count() > 1;
    }

    return false;
  }
}
