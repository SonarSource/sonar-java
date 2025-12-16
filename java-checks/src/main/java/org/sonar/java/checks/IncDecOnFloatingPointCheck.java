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

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S8346")
public class IncDecOnFloatingPointCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.POSTFIX_INCREMENT,
      Tree.Kind.PREFIX_INCREMENT,
      Tree.Kind.POSTFIX_DECREMENT,
      Tree.Kind.PREFIX_DECREMENT
    );
  }

  @Override
  public void visitNode(Tree tree) {
    Optional.of(tree)
      .filter(UnaryExpressionTree.class::isInstance)
      .map(UnaryExpressionTree.class::cast)
      .filter(unaryExpr ->
        unaryExpr.expression() instanceof IdentifierTree identifierTree
          && isFloatingPoint(identifierTree.symbolType())
      )
      .ifPresent(unaryExpr -> reportIssue(
        unaryExpr,
        "%s operator (%s) should not be used with floating point variables".formatted(
          isIncrement(unaryExpr) ? "Increment" : "Decrement",
          unaryExpr.operatorToken().text()
        )
      ));
  }


  private static boolean isFloatingPoint(Type type) {
    return type.isPrimitive(Primitives.FLOAT) || type.isPrimitive(Primitives.DOUBLE);
  }

  private static boolean isIncrement(UnaryExpressionTree unaryExp) {
    return unaryExp.is(
      Tree.Kind.PREFIX_INCREMENT,
      Tree.Kind.POSTFIX_INCREMENT
    );
  }
}
