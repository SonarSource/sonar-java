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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Rule(key = "S3358")
public class NestedTernaryOperatorsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    ConditionalExpressionTree ternary = (ConditionalExpressionTree) tree;
    Stream.of(ternary.condition(), ternary.trueExpression(), ternary.falseExpression()).forEach(expr -> expr.accept(new TernaryVisitor()));

  }

  private class TernaryVisitor extends BaseTreeVisitor {
    @Override
    public void visitConditionalExpression(ConditionalExpressionTree tree) {
      // cut the exploration to report only 1 level
      reportIssue(tree, "Extract this nested ternary operation into an independent statement.");
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip nested anonymous classes which could be declared within condition or true/false branches, and using ternary operator
    }
  }

}
