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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6203")
public class TextBlocksInComplexExpressionsCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Move this text block out of the lambda body and refactor it to a local variable or a static final field.";
  private static final int DEFAULT_LINES_NUMBER = 5;
  @RuleProperty(key = "MaximumNumberOfLines", 
    description = "The maximum number of lines in a text block that can be nested into a complex expression.",
    defaultValue = "" + DEFAULT_LINES_NUMBER)
  private int linesNumber = DEFAULT_LINES_NUMBER;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) tree;
    if (lambdaExpressionTree.parent().is(Tree.Kind.ARGUMENTS)) {
      TextBlockFinder finder = new TextBlockFinder(linesNumber);
      lambdaExpressionTree.body().accept(finder);
      finder.misusedTextBlocks.forEach(textBlock -> reportIssue(textBlock, MESSAGE));
    }
  }

  public void setLinesNumber(int linesNumber) {
    this.linesNumber = linesNumber;
  }

  private static final class TextBlockFinder extends BaseTreeVisitor {

    private final int maxLines;
    
    public TextBlockFinder(int maxLines) {
      this.maxLines = maxLines;
    }

    private final List<Tree> misusedTextBlocks = new ArrayList<>();

    @Override
    public void visitLiteral(LiteralTree tree) {
      if (tree.is(Tree.Kind.TEXT_BLOCK)) {
        String value = tree.value();
        if (value.split("\r?\n|\r").length > maxLines) {
          misusedTextBlocks.add(tree);
        }
      }
    }
  }
}
