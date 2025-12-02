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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.matcher.TreeMatcher;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.matcher.TreeMatcher.hasSize;
import static org.sonar.java.matcher.TreeMatcher.isLambdaExpression;
import static org.sonar.java.matcher.TreeMatcher.withBody;

@Rule(key = "S7629")
public class DefaultFinisherInGathererFactoryCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Remove the default finisher from this Gatherer factory.";

  private final MethodMatchers ofSequentialMatchers = MethodMatchers.create()
    .ofTypes("java.util.stream.Gatherer")
    .names("ofSequential")
    .addParametersMatcher("java.util.function.Supplier",
      "java.util.stream.Gatherer$Integrator", "java.util.function.BiConsumer")
    .addParametersMatcher(
      "java.util.stream.Gatherer$Integrator", "java.util.function.BiConsumer")
    .build();

  private final MethodMatchers defaultFinisherMatchers = MethodMatchers.create()
    .ofTypes("java.util.stream.Gatherer")
    .names("defaultFinisher")
    .addParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (ofSequentialMatchers.matches(mit)) {
      ExpressionTree lastArgument = mit.arguments().get(mit.arguments().size() - 1);
      if (TreeMatcher.calls(defaultFinisherMatchers)
        .or(isLambdaExpression(withBody(hasSize(0)))).check(lastArgument)) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(lastArgument)
          .withMessage(ISSUE_MESSAGE)
          .withQuickFix(() -> computeQuickFix(mit.arguments()))
          .report();
      }
    }
  }

  private static JavaQuickFix computeQuickFix(Arguments arguments) {
    return JavaQuickFix
      .newQuickFix("Remove finisher argument.")
      .addTextEdit(JavaTextEdit.replaceBetweenTree(
        arguments.get(arguments.size() - 2), false, arguments.get(arguments.size() - 1), true, ""))
      .build();
  }

}
