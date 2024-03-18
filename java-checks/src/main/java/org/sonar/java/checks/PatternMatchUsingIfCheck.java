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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.PatternTree;
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
    var topLevelIfStat = (IfStatementTree) tree;

    // Do not report errors more than once for the same chain of ifs
    if (isElseIf(topLevelIfStat) || !(topLevelIfStat.elseStatement() instanceof IfStatementTree)) {
      return;
    }

    // Optimization
    var conditionKind = topLevelIfStat.condition().kind();
    if (conditionKind != Tree.Kind.PATTERN_INSTANCE_OF && conditionKind != Tree.Kind.EQUAL_TO
      && conditionKind != Tree.Kind.CONDITIONAL_AND && conditionKind != Tree.Kind.CONDITIONAL_OR) {
      return;
    }

    var cases = extractCasesFromIf(topLevelIfStat);
    if (cases == null || !casesHaveCommonScrutinee(cases)) {
      return;
    }

    QuickFixHelper.newIssue(context).forRule(this)
      .onTree(topLevelIfStat.ifKeyword())
      .withMessage(ISSUE_MESSAGE)
      .withQuickFix(() -> null) // FIXME
      .report();
  }

  /**
   * Precondition: cases is not empty
   */
  private static boolean casesHaveCommonScrutinee(List<Case> cases) {
    var iter = cases.iterator();
    var scrutinee = iter.next().scrutinee();
    while (iter.hasNext()) {
      if (!iter.next().scrutinee().equals(scrutinee)) {
        return false;
      }
    }
    return true;
  }

  private static @Nullable List<Case> extractCasesFromIf(IfStatementTree topLevelIfStat) {
    var cases = new LinkedList<Case>();
    for (Tree tree = topLevelIfStat; tree instanceof IfStatementTree ifStat; tree = ifStat.elseStatement()) {
      var caze = convertToCase(ifStat.condition());
      if (caze == null) {
        return null;
      }
      cases.add(caze);
    }
    return cases;
  }

  private static @Nullable Case convertToCase(ExpressionTree condition) {
    var leftmost = findLeftmostInConjunction(condition);
    var guards = new LinkedList<ExpressionTree>();
    populateGuardsList(condition, guards);
    if (leftmost instanceof PatternInstanceOfTree patInstOf && patInstOf.pattern() != null && patInstOf.expression() instanceof IdentifierTree idTree) {
      return new PatternMatchCase(idTree.name(), patInstOf.pattern(), guards);
    } else if (leftmost.kind() == Tree.Kind.CONDITIONAL_OR || leftmost.kind() == Tree.Kind.EQUAL_TO) {
      return extractVarAndConstantsFromEqualityChecksDisjunction(leftmost, guards);
    } else {
      return null;
    }
  }

  private static @Nullable EqualityCase extractVarAndConstantsFromEqualityChecksDisjunction(ExpressionTree expr,
                                                                                            List<ExpressionTree> guards) {
    var constantsList = new LinkedList<ExpressionTree>();
    String scrutinee = null;
    while (expr.kind() == Tree.Kind.CONDITIONAL_OR) {
      var binary = (BinaryExpressionTree) expr;
      var varAndCst = extractVarAndConstFromEqualityCheck(binary.rightOperand());
      if (varAndCst == null) {
        return null;
      } else if (scrutinee == null) {
        scrutinee = varAndCst.a;
      } else if (!varAndCst.a.equals(scrutinee)) {
        return null;
      }
      constantsList.add(varAndCst.b);
      expr = binary.leftOperand();
    }
    var varAndCst = extractVarAndConstFromEqualityCheck(expr);
    if (varAndCst == null || (scrutinee != null && !varAndCst.a.equals(scrutinee))) {
      return null;
    }
    constantsList.add(varAndCst.b);
    return new EqualityCase(scrutinee == null ? varAndCst.a : scrutinee, constantsList, guards);
  }

  private static @Nullable Pair<String, ExpressionTree> extractVarAndConstFromEqualityCheck(ExpressionTree expr) {
    if (expr.kind() == Tree.Kind.EQUAL_TO) {
      var binary = (BinaryExpressionTree) expr;
      if (binary.leftOperand() instanceof IdentifierTree idTree
        && (binary.rightOperand().asConstant().isPresent() || binary.rightOperand().symbolType().symbol().isEnum())) {
        return new Pair<>(idTree.name(), binary.rightOperand());
      }
    }
    return null;
  }

  private static ExpressionTree findLeftmostInConjunction(ExpressionTree expr) {
    while (expr.kind() == Tree.Kind.CONDITIONAL_AND) {
      expr = ((BinaryExpressionTree) expr).leftOperand();
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

  private sealed interface Case permits PatternMatchCase, EqualityCase {
    String scrutinee();

    List<ExpressionTree> guards();
  }

  private record PatternMatchCase(String scrutinee, PatternTree pattern, List<ExpressionTree> guards) implements Case {
  }

  private record EqualityCase(String scrutinee, List<ExpressionTree> constants, List<ExpressionTree> guards) implements Case {
  }

  private record Pair<A, B>(A a, B b) {
  }

}
