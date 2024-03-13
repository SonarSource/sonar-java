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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.Tree;


// FIXME use InstanceOfTreeImpl?


@Rule(key = "S6880")
public class PatternMatchUsingIfCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String ISSUE_MESSAGE = "Replace the chain of if/else with a switch expression.";
  private static final int INDENT = 2;

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    var ifStat = (IfStatementTree) tree;

    // Do not report errors more than once for the same chain of ifs
    if (isElseIf(ifStat)) {
      return;
    }

    if (isFaultyIfSequence(ifStat)){
      QuickFixHelper.newIssue(context).forRule(this)
        .onTree(ifStat.ifKeyword())
        .withMessage(ISSUE_MESSAGE)
        .withQuickFix(() -> computeQuickFix(ifStat))
        .report();
    }
  }

  private static boolean isFaultyIfSequence(IfStatementTree topLevelIfStat){
    var elseStat = topLevelIfStat.elseStatement();
    if (!(elseStat instanceof IfStatementTree)){
      return false;
    }
    var targetVarName = findScrutineeInCondition(topLevelIfStat.condition());
    if (targetVarName == null){
      return false;
    }
    for (Tree tree = elseStat; tree instanceof IfStatementTree elseIfStat; tree = elseIfStat.elseStatement()){
      if (!(elseIfStat.condition() instanceof InstanceOfTreeImpl instOf && instOf.expression() instanceof IdentifierTree idTree
        && idTree.name().equals(targetVarName))){
        return false;
      }
    }
    return true;
  }

  private static @Nullable String findScrutineeInCondition(ExpressionTree condition){
    var leftmost = findLeftmostInConjunction(condition);
    if (leftmost instanceof InstanceOfTreeImpl instOf && instOf.expression() instanceof IdentifierTree idTree){
      return idTree.name();
    } else {
      return null;
    }
  }

  private static ExpressionTree findLeftmostInConjunction(ExpressionTree expr) {
    if (expr instanceof BinaryExpressionTree binary) {
      return findLeftmostInConjunction(binary.leftOperand());
    }
    return expr;
  }

  private static boolean isElseIf(IfStatementTree ifStat) {
    return ifStat.parent() instanceof IfStatementTree parentIf && parentIf.elseStatement() == ifStat;
  }

  private JavaQuickFix computeQuickFix(IfStatementTree topLevelIfStat){
    var baseIndent = topLevelIfStat.firstToken().range().start().column() - 1;
    var targetName = scrutineeVarName(findLeftmostInConjunction(topLevelIfStat.condition()));
    var sb = new StringBuilder();
    sb.append("switch (").append(targetName).append(") {\n");
    Tree tree;
    for (tree = topLevelIfStat; tree instanceof IfStatementTree ifStat; tree = ifStat.elseStatement()){
      sb.append(makeCase(ifStat, baseIndent).indent(INDENT));
    }
    if (tree != null){
      sb.append(makeDefault(tree, baseIndent).indent(INDENT));
    }
    sb.append("}");
    var quickFixBuilder = JavaQuickFix.newQuickFix(ISSUE_MESSAGE);
    quickFixBuilder.addTextEdit(JavaTextEdit.replaceTree(topLevelIfStat, sb.toString()));
    return quickFixBuilder.build();
  }

  private String makeCase(IfStatementTree ifStat, int baseIndent){
    var instOf = (InstanceOfTreeImpl) findLeftmostInConjunction(ifStat.condition());
    var sb = new StringBuilder();
    sb.append(" ".repeat(baseIndent)).append("case ");
    var pattern = instOf.pattern();
    if (pattern == null){
      sb.append(QuickFixHelper.contentForTree(instOf.type(), context)).append(" ignored");
    } else {
      sb.append(QuickFixHelper.contentForTree(pattern, context));
    }
    var guardsList = new LinkedList<ExpressionTree>();
    populateGuardsList(ifStat.condition(), guardsList);
    if (!guardsList.isEmpty()){
      var guardsIter = guardsList.iterator();
      while (guardsIter.hasNext()){
        sb.append(QuickFixHelper.contentForTree(guardsIter.next(), context));
        if (guardsIter.hasNext()){
          sb.append(" && ");
        }
      }
    }
    sb.append(" -> ").append(QuickFixHelper.contentForTree(ifStat.thenStatement(), context));
    return sb.toString();
  }

  private String makeDefault(Tree defaultCaseBody, int baseIndent){
    var sb = new StringBuilder();
    sb.append(" ".repeat(baseIndent)).append("default -> ").append(QuickFixHelper.contentForTree(defaultCaseBody, context));
    return sb.toString();
  }

  private static String scrutineeVarName(ExpressionTree instanceOfExpr){
    var instOf = (InstanceOfTreeImpl) instanceOfExpr;
    var idTree = ((IdentifierTree) instOf.expression());
    return idTree.name();
  }

  private static void populateGuardsList(ExpressionTree expr, Deque<ExpressionTree> guards) {
    if (expr instanceof BinaryExpressionTree binary && binary.kind() == Tree.Kind.CONDITIONAL_AND) {
      guards.addFirst(binary.rightOperand());
      populateGuardsList(binary.leftOperand(), guards);
    }
  }

}
