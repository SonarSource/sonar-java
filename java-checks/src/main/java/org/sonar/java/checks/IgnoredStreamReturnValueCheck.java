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
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2674")
public class IgnoredStreamReturnValueCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("java.io.InputStream")
      .names("skip")
      .addParametersMatcher("long")
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.io.InputStream")
      .names("read")
      .addParametersMatcher("byte[]")
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree statement = ((ExpressionStatementTree) tree).expression();
    if (statement.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) statement;
      if (MATCHERS.matches(mit)) {
        reportIssue(ExpressionUtils.methodName(mit), "Check the return value of the \"" + mit.methodSymbol().name() + "\" call to see how many bytes were read.");
      }
    }
  }

}
