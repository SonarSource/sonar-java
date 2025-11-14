/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.unused;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Check for collections whose content is not used,
 * that is, the only operations on them are adding and removing elements.
 */
@Rule(key = "S4030")
public class UnusedCollectionCheck extends IssuableSubscriptionVisitor {
  private static final Set<String> MUTATE_METHODS = Set.of("add", "addAll", "clear", "remove", "removeAll", "removeIf", "retainAll");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof VariableTree variableTree) {
      Symbol symbol = variableTree.symbol();

      if (symbol.type().isSubtypeOf("java.util.Collection") &&
        symbol.isLocalVariable() &&
        isInitializedByConstructor(variableTree.initializer()) &&
        // Do not raise without any usage: usages() may be unreliable,
        // moreover, this case is in scope of S1481 and S1854 (unused variable and assignment)
        !symbol.usages().isEmpty() &&
        symbol.usages().stream().allMatch(UnusedCollectionCheck::isWriteOnly)) {
        reportIssue(variableTree.simpleName(), "Consume or remove this unused collection");
      }
    }
  }

  /**
   * Verify that the initializer is a call to a constructor, for example `new ArrayList()`,
   * and not a method call or missing, which happens when the variable is a parameter.
   */
  private static boolean isInitializedByConstructor(@Nullable ExpressionTree initializer) {
    return initializer instanceof NewClassTree;
  }

  /**
   * Check that we have a call to one of the mutate methods and the return value is ignored.
   */
  private static boolean isWriteOnly(Tree tree) {
    return Optional.ofNullable(tree)
      .map(Tree::parent)
      .filter(MemberSelectExpressionTree.class::isInstance)
      .map(MemberSelectExpressionTree.class::cast)
      .filter(memberSelectExpressionTree -> MUTATE_METHODS.contains(memberSelectExpressionTree.identifier().name()))
      .map(Tree::parent)
      .filter(MethodInvocationTree.class::isInstance)
      .map(Tree::parent)
      .filter(ExpressionStatementTree.class::isInstance)
      .map(Tree::parent)
      .filter(BlockTree.class::isInstance)
      .isPresent();
  }
}
