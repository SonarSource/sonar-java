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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.TreeMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.matcher.TreeMatcher.calls;
import static org.sonar.java.matcher.TreeMatcher.hasSize;
import static org.sonar.java.matcher.TreeMatcher.invokedOn;
import static org.sonar.java.matcher.TreeMatcher.isExpression;
import static org.sonar.java.matcher.TreeMatcher.isIdentifier;
import static org.sonar.java.matcher.TreeMatcher.isInvocationOf;
import static org.sonar.java.matcher.TreeMatcher.statementAt;

@Rule(key = "S7479")
public class ClassBuilderWithMethodCheck extends IssuableSubscriptionVisitor {

  private final MethodMatchers withMethod = MethodMatchers.create()
    .ofTypes("java.lang.classfile.ClassBuilder")
    .names("withMethod")
    .withAnyParameters()
    .build();

  private final MethodMatchers withCode = MethodMatchers.create()
    .ofTypes("java.lang.classfile.MethodBuilder")
    .names("withCode")
    .withAnyParameters()
    .build();

  /** Matches lambda bodies composed of just a call to `MethodBuilder.withBody` on the given variable. */
  private TreeMatcher<LambdaExpressionTree> makeMatcher(VariableTree variableCalledOn) {
    return TreeMatcher.withBody(
      // Case with curly braces
      hasSize(1)
        .and(
          statementAt(0, isInvocationOf(withCode, invokedOn(isIdentifier(variableCalledOn.symbol())))))
        // Case with no curly braces
        .or(isExpression(calls(withCode, invokedOn(isIdentifier(variableCalledOn.symbol()))))));
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  /** Raise if last argument of withMethod is a lambda which only calls `MethodBuilder.withCode` */
  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (withMethod.matches(invocation)) {
      ExpressionTree lastArgument = invocation.arguments().get(invocation.arguments().size() - 1);

      if (lastArgument instanceof LambdaExpressionTree lambda) {
        VariableTree parameter = lambda.parameters().get(0);
        if (makeMatcher(parameter).check(lambda)) {
          reportIssue(findLocation(invocation), "Replace call with `ClassBuilder.withMethodBody`.");
        }
      }
    }
  }

  private static Tree findLocation(MethodInvocationTree invocation) {
    ExpressionTree methodSelect = invocation.methodSelect();
    return ((MemberSelectExpressionTree) methodSelect).identifier();
  }

}
