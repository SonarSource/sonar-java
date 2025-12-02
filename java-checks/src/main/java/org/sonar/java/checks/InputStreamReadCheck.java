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
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4517")
public class InputStreamReadCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers INPUT_STREAM_READ = MethodMatchers.create()
    .ofSubTypes("java.io.InputStream").names("read").addWithoutParametersMatcher().build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    if (method.block() != null && INPUT_STREAM_READ.matches(method)) {
      method.accept(new ReturnStatementVisitor());
    }
  }

  private class ReturnStatementVisitor extends BaseTreeVisitor {

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      ExpressionTree expression = tree.expression();
      if (expression.symbolType().is("byte")) {
        reportIssue(expression, "Convert this signed byte into an unsigned byte.");
      }
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip inner or annonymous classes
    }
  }
}
