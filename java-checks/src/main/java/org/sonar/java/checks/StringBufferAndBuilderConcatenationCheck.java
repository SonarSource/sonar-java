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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

/**
 * Suggest that <code>stringBuilder.append("text1" + "text2")</code> is replaced with
 * <code>stringBuilder.append("text1").append("text2")</code>.
 */
@Rule(key = "S3024")
public class StringBufferAndBuilderConcatenationCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_STRING = "java.lang.String";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.lang.StringBuffer", "java.lang.StringBuilder")
      .names("append")
      .addParametersMatcher(JAVA_LANG_STRING)
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree arg = mit.arguments().get(0);
    BinaryExpressionTree binaryExpressionTree = getConcatenationTree(arg);

    if (binaryExpressionTree != null && isInsideLoop(mit)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(arg)
        .withMessage("Use multiple calls to \"append\" instead of string concatenation.")
        .withQuickFix(() -> getQuickFix(mit.arguments()))
        .report();
    }
  }

  private static boolean isInsideLoop(Tree tree) {
    while (tree != null) {
      if (tree instanceof ForStatementTree || tree instanceof ForEachStatement
        || tree instanceof WhileStatementTree || tree instanceof DoWhileStatementTree) {
        return true;
      }
      if (tree instanceof MethodTree) {
        break;
      }
      tree = tree.parent();
    }
    return false;
  }

  /**
   * If the tree represents <code>arg1 + arg2</code>, then return <code>Optional.of(tree)</code>,
   * otherwise return <code>Optional.empty()</code>.
   * Using optional, rather than if-then, helps us avoid some problems in coverage by unit tests.
   */
  private static @Nullable BinaryExpressionTree getConcatenationTree(ExpressionTree tree) {
    return Optional.of(tree)
      .filter(BinaryExpressionTree.class::isInstance)
      .map(BinaryExpressionTree.class::cast)
      .filter(bet -> bet.is(Tree.Kind.PLUS))
      .orElse(null);
  }

  private JavaQuickFix getQuickFix(Arguments arguments) {
    String replacement = splitExpressionOnPlus(arguments.get(0)).stream()
      .map(ExpressionUtils::skipParentheses)
      .map(tree -> "(" + QuickFixHelper.contentForTree(tree, context) + ")")
      .collect(Collectors.joining(".append"));

    return JavaQuickFix.newQuickFix("Call \"append\" multiple times.")
      .addTextEdit(JavaTextEdit.replaceTree(arguments, replacement))
      .build();
  }

  private static List<ExpressionTree> splitExpressionOnPlus(ExpressionTree tree) {
    List<ExpressionTree> accumulator = new ArrayList<>();
    splitExpressionOnPlus(accumulator, tree);
    return accumulator;
  }

  private static void splitExpressionOnPlus(List<ExpressionTree> accumulator, ExpressionTree tree) {
    BinaryExpressionTree bet = getConcatenationTree(tree);
    if (bet != null) {
      ExpressionTree left = bet.leftOperand();
      ExpressionTree right = bet.rightOperand();
      splitExpressionOnPlus(accumulator, left);
      accumulator.add(right);
    } else {
      accumulator.add(tree);
    }
  }
}
