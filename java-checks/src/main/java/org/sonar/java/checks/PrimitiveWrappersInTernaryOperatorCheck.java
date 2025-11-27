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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2154")
public class PrimitiveWrappersInTernaryOperatorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    ConditionalExpressionTree cet = (ConditionalExpressionTree) tree;
    Type trueExpressionType = cet.trueExpression().symbolType();
    Type falseExpressionType = cet.falseExpression().symbolType();
    if (dissimilarPrimitiveTypeWrappers(trueExpressionType, falseExpressionType)) {
      reportIssue(cet.questionToken(), "Add an explicit cast to match types of operands.");
    }
  }

  private static boolean dissimilarPrimitiveTypeWrappers(Type trueExprType, Type falseExprType) {
    return trueExprType.isPrimitiveWrapper() && falseExprType.isPrimitiveWrapper() && !trueExprType.equals(falseExprType);
  }

}
