/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.prettyprint.FileConfig;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.ExpressionUtils.skipParentheses;
import static org.sonar.java.prettyprint.PrintableNodesCreation.assignment;
import static org.sonar.java.prettyprint.PrintableNodesCreation.block;
import static org.sonar.java.prettyprint.PrintableNodesCreation.exprStat;
import static org.sonar.java.prettyprint.PrintableNodesCreation.ifStat;
import static org.sonar.java.prettyprint.PrintableNodesCreation.varDecl;

@Rule(key = "S3358")
public class NestedTernaryOperatorsCheck extends IssuableSubscriptionVisitor {
  private static final String ERROR_MESSAGE = "Extract this nested ternary operation into an independent statement.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    ConditionalExpressionTree ternary = (ConditionalExpressionTree) tree;
    if ((skipParentheses(ternary.trueExpression()) instanceof ConditionalExpressionTree || skipParentheses(ternary.falseExpression()) instanceof ConditionalExpressionTree)
      && ternary.parent() instanceof VariableTree varTree) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(tree)
        .withMessage(ERROR_MESSAGE)
        .withQuickFix(() -> computeQuickfix(varTree, ternary))
        .report();
    }
  }

  private JavaQuickFix computeQuickfix(VariableTree varTree, ConditionalExpressionTree ternary) {
    var uninitVarDecl = varDecl(varTree.modifiers(), varTree.type(), varTree.simpleName());
    var ifStat = transform(ternary, varTree.simpleName(), false);
    return JavaQuickFix.newQuickFix(ERROR_MESSAGE)
      .addTextEdit(JavaTextEdit.replaceTreeWithStatsSeq(varTree, List.of(uninitVarDecl, ifStat), FileConfig.DEFAULT_FILE_CONFIG))
      .build();
  }

  private StatementTree transform(ExpressionTree expr, ExpressionTree varName, boolean makeBlock) {
    expr = skipParentheses(expr);
    if (expr instanceof ConditionalExpressionTree ternary) {
      var raw = ifStat(ternary.condition(),
        transform(ternary.trueExpression(), varName, true),
        transform(ternary.falseExpression(), varName, false)
      );
      return makeBlock ? block(raw) : raw;
    } else {
      return block(exprStat(assignment(varName, expr)));
    }
  }

}
