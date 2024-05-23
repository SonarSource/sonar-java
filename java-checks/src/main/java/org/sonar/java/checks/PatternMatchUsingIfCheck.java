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
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.prettyprint.FileConfig;
import org.sonar.java.checks.prettyprint.PrettyPrintStringBuilder;
import org.sonar.java.checks.prettyprint.Prettyprinter;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.sonar.java.checks.prettyprint.PrintableNodesCreation.block;
import static org.sonar.java.checks.prettyprint.PrintableNodesCreation.returnStat;
import static org.sonar.java.checks.prettyprint.PrintableNodesCreation.switchCase;
import static org.sonar.java.checks.prettyprint.PrintableNodesCreation.switchCaseFromLabels;
import static org.sonar.java.checks.prettyprint.PrintableNodesCreation.switchDefault;
import static org.sonar.java.checks.prettyprint.PrintableNodesCreation.switchExpr;
import static org.sonar.java.checks.prettyprint.PrintableNodesCreation.switchStat;


@Rule(key = "S6880")
public class PatternMatchUsingIfCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String ISSUE_MESSAGE = "Replace the chain of if/else with a switch expression.";
  private static final Set<String> SCRUTINEE_TYPES_FOR_NON_PATTERN_SWITCH = Set.of(
    "byte", "short", "char", "int",
    "java.lang.Byte", "java.lang.Short", "java.lang.Character", "java.lang.Integer"
  );

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    var topLevelIfStat = (IfStatementTree) tree;

    if (isElseIf(topLevelIfStat) || !hasElseIf(topLevelIfStat)) {
      return;
    }

    // Optimization
    if (!topLevelIfStat.condition().is(Kind.PATTERN_INSTANCE_OF, Kind.EQUAL_TO, Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR)) {
      return;
    }

    var cases = extractCasesFromIfSequence(topLevelIfStat);
    if (cases == null || !isDefault(cases.get(cases.size() - 1).caseGroupTree) || !casesHaveCommonScrutinee(cases)
      || (isEqualityCase(cases.get(0).caseGroupTree) && !hasValidScrutineeTypeForNonPatternSwitch(cases.get(0).scrutinee()))) {
      return;
    }

    QuickFixHelper.newIssue(context).forRule(this)
      .onTree(topLevelIfStat.ifKeyword())
      .withMessage(ISSUE_MESSAGE)
      .withQuickFix(() -> computeQuickFix(cases, topLevelIfStat))
      .report();
  }

  private static boolean isDefault(CaseGroupTree caseGroupTree) {
    return caseGroupTree.labels().get(0).expressions().isEmpty();
  }

  private static boolean isEqualityCase(CaseGroupTree caseGroupTree) {
    var expressions = caseGroupTree.labels().get(0).expressions();
    return !expressions.isEmpty() && !(expressions.get(0) instanceof PatternTree);
  }

  private static boolean casesHaveCommonScrutinee(List<ScrutineeAndCase> cases) {
    return cases.stream().allMatch(c -> c.scrutinee().name().equals(cases.get(0).scrutinee().name()));
  }

  private static boolean hasValidScrutineeTypeForNonPatternSwitch(IdentifierTree scrutinee) {
    if (scrutinee.symbolType().symbol().isEnum()) {
      return true;
    }
    var fullyQualifiedTypeName = scrutinee.symbolType().fullyQualifiedName();
    return SCRUTINEE_TYPES_FOR_NON_PATTERN_SWITCH.contains(fullyQualifiedTypeName);
  }

  private static @Nullable List<ScrutineeAndCase> extractCasesFromIfSequence(IfStatementTree topLevelIfStat) {
    var cases = new LinkedList<ScrutineeAndCase>();
    StatementTree stat;
    for (stat = topLevelIfStat; stat instanceof IfStatementTree ifStat; stat = ifStat.elseStatement()) {
      var caze = convertToCase(ifStat.condition(), ifStat.thenStatement());
      if (caze == null) {
        return null;
      }
      cases.add(caze);
    }
    if (stat != null) {
      cases.add(new ScrutineeAndCase(cases.getLast().scrutinee(), switchDefault(stat)));
    }
    return cases;
  }

  private static @Nullable ScrutineeAndCase convertToCase(ExpressionTree condition, StatementTree body) {
    var leftmost = findLeftmostInConjunction(condition);
    var guards = new LinkedList<ExpressionTree>();
    populateGuardsList(condition, guards);
    if (leftmost instanceof PatternInstanceOfTree patInstOf && patInstOf.pattern() != null
      && patInstOf.expression() instanceof IdentifierTree scrutinee) {
      var caze = switchCase(patInstOf.pattern(), guards, body);
      return new ScrutineeAndCase(scrutinee, caze);
    } else if ((leftmost.kind() == Kind.CONDITIONAL_OR || leftmost.kind() == Kind.EQUAL_TO) && guards.isEmpty()) {
      return buildEqualityCase(leftmost, body);
    } else {
      return null;
    }
  }

  /**
   * Transforms expressions of the form  a == 0 || a == 1 || ...  into case
   */
  private static @Nullable ScrutineeAndCase buildEqualityCase(ExpressionTree expr, StatementTree body) {
    var constantsList = new LinkedList<ExpressionTree>();
    IdentifierTree scrutinee = null;
    while (expr.kind() == Kind.CONDITIONAL_OR) {
      var binary = (BinaryExpressionTree) expr;
      var currEqCheck = extractVarAndConstFromEqualityCheck(binary.rightOperand());
      if (currEqCheck == null) {
        return null;
      } else if (scrutinee == null) {
        scrutinee = currEqCheck.variable;
      } else if (!currEqCheck.variable.name().equals(scrutinee.name())) {
        return null;
      }
      constantsList.addFirst(currEqCheck.constant);
      expr = binary.leftOperand();
    }
    var varAndCst = extractVarAndConstFromEqualityCheck(expr);
    if (varAndCst == null || (scrutinee != null && !varAndCst.variable.name().equals(scrutinee.name()))) {
      return null;
    }
    constantsList.addFirst(varAndCst.constant);
    scrutinee = scrutinee == null ? varAndCst.variable : scrutinee;
    return new ScrutineeAndCase(scrutinee, switchCase(constantsList, body));
  }

  private static @Nullable ExtractedEqualityCheck extractVarAndConstFromEqualityCheck(ExpressionTree expr) {
    if (expr.kind() == Kind.EQUAL_TO) {
      var binary = (BinaryExpressionTree) expr;
      if (binary.leftOperand() instanceof IdentifierTree idTree && isPossibleConstantForCase(binary.rightOperand())) {
        return new ExtractedEqualityCheck(idTree, binary.rightOperand());
      } else if (binary.rightOperand() instanceof IdentifierTree idTree && isPossibleConstantForCase(binary.leftOperand())) {
        return new ExtractedEqualityCheck(idTree, binary.leftOperand());
      }
    }
    return null;
  }

  private static boolean isPossibleConstantForCase(ExpressionTree expr) {
    return expr.asConstant().isPresent() || expr.symbolType().symbol().isEnum();
  }

  private static ExpressionTree findLeftmostInConjunction(ExpressionTree expr) {
    while (expr.kind() == Kind.CONDITIONAL_AND) {
      expr = ((BinaryExpressionTree) expr).leftOperand();
    }
    return expr;
  }

  private static void populateGuardsList(ExpressionTree expr, Deque<ExpressionTree> guards) {
    while (expr instanceof BinaryExpressionTree binary && binary.kind() == Kind.CONDITIONAL_AND) {
      guards.addFirst(binary.rightOperand());
      expr = binary.leftOperand();
    }
  }

  private static boolean isElseIf(IfStatementTree ifStat) {
    return ifStat.parent() instanceof IfStatementTree parentIf && parentIf.elseStatement() == ifStat;
  }

  private static boolean hasElseIf(IfStatementTree ifStat) {
    return ifStat.elseStatement() instanceof IfStatementTree;
  }

  private JavaQuickFix computeQuickFix(List<ScrutineeAndCase> cases, IfStatementTree topLevelIfStat) {
    var canLiftReturn = cases.stream().allMatch(caze -> canLiftReturn(caze.caseGroupTree));
    var scrutinee = cases.get(0).scrutinee();
    var casesAsTrees = cases.stream()
      .map(ScrutineeAndCase::caseGroupTree)
      .map(c -> switchCaseFromLabels(
        c.labels().stream().map(CaseLabelTreeImpl.class::cast).toList(),
        liftReturnIfRequested(c.body().get(0), canLiftReturn))
      ).toList();
    var switchTree = canLiftReturn ? returnStat(switchExpr(scrutinee, casesAsTrees)) : switchStat(scrutinee, casesAsTrees);
    var ppsb = new PrettyPrintStringBuilder(FileConfig.DEFAULT_FILE_CONFIG, topLevelIfStat.firstToken(), false);
    switchTree.accept(new Prettyprinter(ppsb));
    var edit = JavaTextEdit.replaceTree(topLevelIfStat, ppsb.toString());
    return JavaQuickFix.newQuickFix(ISSUE_MESSAGE).addTextEdit(edit).build();
  }

  private static boolean canLiftReturn(CaseGroupTree caze) {
    if (caze.body().size() != 1) {
      return false;
    }
    var stat = caze.body().get(0);
    return (stat instanceof ReturnStatementTree returnStat && returnStat.expression() != null) || stat instanceof ThrowStatementTree;
  }

  private static StatementTree liftReturnIfRequested(StatementTree body, boolean lift) {
    if (lift && body instanceof ReturnStatementTree returnStat) {
      return new ExpressionStatementTreeImpl(returnStat.expression(), null);
    } else {
      return body;
    }
  }

  private record ScrutineeAndCase(IdentifierTree scrutinee, CaseGroupTreeImpl caseGroupTree) {
  }

  private record ExtractedEqualityCheck(IdentifierTree variable, ExpressionTree constant) {
  }

}
