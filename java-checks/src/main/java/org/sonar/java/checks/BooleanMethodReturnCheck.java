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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2447")
public class BooleanMethodReturnCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    SymbolMetadata metadata = methodTree.symbol().metadata();
    if (returnsBoolean(methodTree) && !metadata.nullabilityData().isNullable(NullabilityLevel.PACKAGE, false, true)) {
      methodTree.accept(new ReturnStatementVisitor());
    }
  }

  private static boolean returnsBoolean(MethodTree methodTree) {
    return methodTree.returnType().symbolType().is("java.lang.Boolean");
  }

  private class ReturnStatementVisitor extends BaseTreeVisitor {

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (tree.expression().is(Kind.NULL_LITERAL)) {
        reportIssue(tree.expression(), "Null is returned but a \"Boolean\" is expected.");
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes as methods of inner classes will be visited by main visitor
    }
  }

}
