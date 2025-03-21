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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.HashSet;
import java.util.Set;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "ForLoopCounterChangedCheck", repositoryKey = "squid")
@Rule(key = "S127")
public class ForLoopCounterChangedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Set<String> loopCounters = new HashSet<>();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    loopCounters.clear();
    scan(context.getTree());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    Set<String> pendingLoopCounters = new HashSet<>();
    for (StatementTree statementTree : tree.initializer()) {
      if (statementTree.is(Tree.Kind.VARIABLE)) {
        pendingLoopCounters.add(((VariableTree) statementTree).simpleName().name());
      }
    }
    scan(tree.initializer());
    scan(tree.condition());
    scan(tree.update());
    loopCounters.addAll(pendingLoopCounters);
    scan(tree.statement());
    loopCounters.removeAll(pendingLoopCounters);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
      checkIdentifier((IdentifierTree) tree.variable());
    }
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if ((isIncrement(tree) || isDecrement(tree)) && tree.expression().is(Tree.Kind.IDENTIFIER)) {
      checkIdentifier((IdentifierTree) tree.expression());
    }
    super.visitUnaryExpression(tree);
  }

  private static boolean isIncrement(UnaryExpressionTree tree) {
    return tree.is(Tree.Kind.PREFIX_INCREMENT) || tree.is(Tree.Kind.POSTFIX_INCREMENT);
  }

  private static boolean isDecrement(UnaryExpressionTree tree) {
    return tree.is(Tree.Kind.POSTFIX_DECREMENT) || tree.is(Tree.Kind.PREFIX_DECREMENT);
  }

  private void checkIdentifier(IdentifierTree identifierTree) {
    if (loopCounters.contains(identifierTree.name())) {
      context.reportIssue(this, identifierTree, "Refactor the code in order to not assign to this loop counter from within the loop body.");
    }
  }

}
