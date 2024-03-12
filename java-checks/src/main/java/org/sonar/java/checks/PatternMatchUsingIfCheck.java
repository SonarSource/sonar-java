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
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S6880")
public class PatternMatchUsingIfCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String ISSUE_MESSAGE = "Replace the chain of if/else with a switch expression.";

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

    var cases = computeCases(ifStat);
    if (cases == null) {
      return;
    }
    var scrutVarName = commonScrutineeVariableName(cases);
    if (scrutVarName == null) {
      return;
    }

    QuickFixHelper.newIssue(context).forRule(this)
      .onTree(ifStat.ifKeyword())
      .withMessage(ISSUE_MESSAGE)
      .withQuickFix(() -> computeQuickFix(ifStat, cases, scrutVarName))
      .report();
  }

  private record Case(PatternInstanceOfTree instOf, Deque<ExpressionTree> guards, Tree body) {
  }

  private static @Nullable List<Case> computeCases(IfStatementTree ifTree) {
    // Do not report issue if only one condition
    if (!(ifTree.elseStatement() instanceof IfStatementTree)) {
      return null;
    }
    var cases = new LinkedList<Case>();
    for (Tree tree = ifTree; tree instanceof IfStatementTree ifStat; tree = ifStat.elseStatement()) {
      var caze = convertToCase(ifStat.condition(), ifStat.thenStatement());
      if (caze == null) {
        return null;
      }
      cases.add(caze);
    }
    return cases;
  }

  private static @Nullable Case convertToCase(ExpressionTree condition, Tree body) {
    var leftmost = findLeftmostInConjunction(condition);
    if (leftmost instanceof PatternInstanceOfTree instOf && instOf.expression() instanceof IdentifierTree) {
      var guards = new LinkedList<ExpressionTree>();
      populateGuardsList(condition, guards);
      return new Case(instOf, guards, body);
    }
    return null;
  }

  private static Tree findLeftmostInConjunction(ExpressionTree expr) {
    if (expr instanceof BinaryExpressionTree binary) {
      return findLeftmostInConjunction(binary.leftOperand());
    }
    return expr;
  }

  private static void populateGuardsList(ExpressionTree expr, Deque<ExpressionTree> guards) {
    if (expr instanceof BinaryExpressionTree binary && binary.kind() == Tree.Kind.CONDITIONAL_AND) {
      guards.addFirst(binary.rightOperand());
      populateGuardsList(binary.leftOperand(), guards);
    }
  }

  private static boolean isElseIf(IfStatementTree ifStat) {
    return ifStat.parent() instanceof IfStatementTree parentIf && parentIf.elseStatement() == ifStat;
  }

  private static @Nullable String commonScrutineeVariableName(List<Case> cases) {
    String scrutineeVarName = null;
    for (var caze : cases) {
      var scrutinee = caze.instOf.expression();
      if (scrutinee instanceof IdentifierTree idTree && scrutineeVarName == null) {
        scrutineeVarName = idTree.name();
      } else if (scrutinee instanceof IdentifierTree idTree && !idTree.name().equals(scrutineeVarName)) {
        return null;
      }
    }
    return scrutineeVarName;
  }

  private JavaQuickFix computeQuickFix(IfStatementTree ifStat, List<Case> cases, String scrutVarName) {
    var quickFixBuilder = JavaQuickFix.newQuickFix(ISSUE_MESSAGE);
    quickFixBuilder.addTextEdit(JavaTextEdit.replaceTree(ifStat, computeReplacement(cases, scrutVarName)));
//    System.out.println(computeReplacement(cases, scrutVarName));  // FIXME remove
    return quickFixBuilder.build();
  }

  private String computeReplacement(List<Case> cases, String scrutVarName) {
    var sb = new StringBuilder();
    sb.append("switch (").append(scrutVarName).append(") {\n");
    for (var caze : cases) {
      writeCase(caze, sb);
      sb.append('\n');
    }
    sb.append("}");
    return sb.toString();
  }

  private void writeCase(Case caze, StringBuilder sb) {
    var pattern = caze.instOf.pattern();
    String casePattern;
    if (pattern == null) {
      casePattern = QuickFixHelper.contentForTree(((InstanceOfTree) caze.instOf).type(), context) + " ignored";
    } else {
      casePattern = QuickFixHelper.contentForTree(pattern, context);
    }
    sb.append("case ").append(casePattern);
    if (!caze.guards.isEmpty()) {
      sb.append(" when ");
      var guardsIter = caze.guards.iterator();
      while (guardsIter.hasNext()) {
        var guard = guardsIter.next();
        sb.append(QuickFixHelper.contentForTree(guard, context));
        if (guardsIter.hasNext()) {
          sb.append(" && ");
        }
      }
    }
    sb.append(" -> ").append(QuickFixHelper.contentForTree(caze.body, context));
  }

}
