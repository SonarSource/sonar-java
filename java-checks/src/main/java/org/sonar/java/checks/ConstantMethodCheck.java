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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S3400")
public class ConstantMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    BlockTree body = methodTree.block();
    boolean isSingleStatementMethod = body != null && body.body().size() == 1;
    boolean hasAnnotations = !methodTree.modifiers().annotations().isEmpty();
    if (!isSingleStatementMethod || hasAnnotations) {
      return;
    }
    if (isEffectivelyFinal(methodTree) && Boolean.FALSE.equals(methodTree.isOverriding())) {
      StatementTree uniqueStatement = body.body().get(0);
      if (uniqueStatement.is(Kind.RETURN_STATEMENT)) {
        ExpressionTree returnedExpression = ((ReturnStatementTree) uniqueStatement).expression();
        if (isConstant(returnedExpression)) {
          reportIssue(returnedExpression, "Remove this method and declare a constant for this value.");
        }
      }
    }
  }

  private static boolean isEffectivelyFinal(MethodTree methodTree) {
    Tree methodParent = methodTree.parent();
    if (!(methodParent instanceof ClassTree) || methodParent.is(Tree.Kind.ANNOTATION_TYPE, Tree.Kind.INTERFACE)) {
      return false;
    } else if (methodParent.is(Tree.Kind.RECORD) || ((ClassTree)methodParent).symbol().isFinal()) {
      return true;
    }
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    return methodSymbol.isFinal() || methodSymbol.isPrivate() || methodSymbol.isStatic();
  }

  private static boolean isConstant(@Nullable ExpressionTree returnedExpression) {
    return returnedExpression != null
            && returnedExpression.is(Kind.INT_LITERAL, Kind.LONG_LITERAL,
            Kind.CHAR_LITERAL, Kind.STRING_LITERAL, Kind.TEXT_BLOCK,
            Kind.DOUBLE_LITERAL, Kind.FLOAT_LITERAL, Kind.BOOLEAN_LITERAL);
  }
}
