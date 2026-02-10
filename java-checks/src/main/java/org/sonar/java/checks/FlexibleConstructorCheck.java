/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.Collections;
import java.util.List;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class FlexibleConstructorCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  /**
   * Validate the constructor body, providing the constructor method tree, the list of statements in the constructor body, and the index of any explicit super() or this() call (or -1 if no explicit call is found).
   * @param constructor the constructor method tree being validated
   * @param body the list of statements in the constructor body
   * @param constructorCallIndex the index of any explicit super() or this() call in the body, or -1 if no explicit call is found (implicit super())
   */
  abstract void validateConstructor(MethodTree constructor, List<StatementTree> body, int constructorCallIndex);

  @Override
  public final List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public final boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  public final void visitNode(Tree tree) {
    MethodTree constructor = (MethodTree) tree;
    BlockTree block = constructor.block();
    if (block == null || block.body().isEmpty()) {
      // No body or empty body, nothing to validate
      return;
    }
    List<StatementTree> body = block.body();

    // Find the super() or this() call
    int constructorCallIndex = findConstructorCallIndex(body);
    validateConstructor(constructor, body, constructorCallIndex);
  }

  /**
   * Find the index of an explicit super() or this() call in the constructor body.
   *
   * @param body the constructor body to search
   * @return the index of the explicit super() or this() call, or -1 if no explicit call is found (implicit super())
   */
  private static int findConstructorCallIndex(List<StatementTree> body) {
    for (int i = 0; i < body.size(); i++) {
      if (body.get(i) instanceof ExpressionStatementTree expressionStatementTree
        && expressionStatementTree.expression() instanceof MethodInvocationTree methodInvocationTree
        && methodInvocationTree.methodSelect() instanceof IdentifierTree identifierTree
        && ExpressionUtils.isThisOrSuper(identifierTree.name())) {
        return i;
      }
    }
    // No explicit super() or this() call
    return -1;
  }
}
