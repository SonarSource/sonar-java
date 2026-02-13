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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8450")
public class BufferedReaderBoilerplateCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final MethodMatchers BUFFERED_READER_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("java.io.BufferedReader")
    .constructor()
    .addParametersMatcher("java.io.Reader")
    .build();

  private static final MethodMatchers INPUT_STREAM_READER_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("java.io.InputStreamReader")
    .constructor()
    .addParametersMatcher("java.io.InputStream")
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    if (!BUFFERED_READER_CONSTRUCTOR.matches(newClassTree)) {
      return;
    }
    ExpressionTree firstArg = newClassTree.arguments().get(0);
    if (firstArg.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree innerNewClass = (NewClassTree) firstArg;
      if (INPUT_STREAM_READER_CONSTRUCTOR.matches(innerNewClass) && isSystemIn(innerNewClass.arguments().get(0))) {
        reportIssue(newClassTree, "Use \"IO.readln()\" instead of this \"BufferedReader\" boilerplate.");
      }
    }
  }

  private static boolean isSystemIn(ExpressionTree expression) {
    if (!expression.is(Tree.Kind.MEMBER_SELECT)) {
      return false;
    }
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
    return "in".equals(memberSelect.identifier().name()) &&
      memberSelect.expression().symbolType().is("java.lang.System");
  }
}
