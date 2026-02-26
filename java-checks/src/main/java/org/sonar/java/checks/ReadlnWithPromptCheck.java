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
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8469")
public class ReadlnWithPromptCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Use \"IO.readln(prompt)\" instead of separate \"IO.%s(prompt)\" and \"IO.readln()\" calls.";

  private static final MethodMatchers PRINT_MATCHERS = MethodMatchers.create()
    .ofTypes("java.lang.IO")
    .names("print", "println")
    .addParametersMatcher("java.lang.Object")
    .build();

  private static final MethodMatchers READLN_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.IO")
    .names("readln")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return READLN_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Tree parent = mit.parent();
    Tree grandParent = parent.parent();
    if (grandParent == null || !grandParent.is(Tree.Kind.BLOCK)) {
      return;
    }

    BlockTree block = (BlockTree) grandParent;
    List<StatementTree> statements = block.body();

    int currentIndex = statements.indexOf(parent);
    if (currentIndex <= 0) {
      return;
    }

    StatementTree previousStatement = statements.get(currentIndex - 1);
    if (previousStatement instanceof ExpressionStatementTree exprStmt
      && exprStmt.expression() instanceof MethodInvocationTree previousCall
      && PRINT_MATCHERS.matches(previousCall)) {
      String message = String.format(MESSAGE, previousCall.methodSymbol().name());
      reportIssue(mit, message);
    }
  }

}
