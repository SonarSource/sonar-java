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

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.prettyprint.FileConfig;
import org.sonar.java.checks.prettyprint.PrettyPrintStringBuilder;
import org.sonar.java.checks.prettyprint.Prettyprinter;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
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
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.sonar.java.checks.prettyprint.Template.caseGroupTemplate;
import static org.sonar.java.checks.prettyprint.Template.expressionTemplate;
import static org.sonar.java.checks.prettyprint.Template.statementTemplate;
import static org.sonar.java.checks.prettyprint.Template.token;


@Rule(key = "S6880")
public class PatternMatchUsingIfCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String ISSUE_MESSAGE = "Replace the chain of if/else with a switch expression.";
  private static final int INDENT = 2;
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
    if (cases == null || !(cases.get(cases.size() - 1) instanceof DefaultCase) || !casesHaveCommonScrutinee(cases)
      || (cases.get(0) instanceof EqualityCase && !hasValidScrutineeTypeForNonPatternSwitch(cases.get(0).scrutinee()))) {
      return;
    }

    QuickFixHelper.newIssue(context).forRule(this)
      .onTree(topLevelIfStat.ifKeyword())
      .withMessage(ISSUE_MESSAGE)
      .withQuickFix(() -> computeQuickFix(cases, topLevelIfStat))
      .report();
  }

  private static boolean casesHaveCommonScrutinee(List<Case> cases) {
    return cases.stream().allMatch(c -> c.scrutinee().name().equals(cases.get(0).scrutinee().name()));
  }

  private static boolean hasValidScrutineeTypeForNonPatternSwitch(IdentifierTree scrutinee) {
    if (scrutinee.symbolType().symbol().isEnum()) {
      return true;
    }
    var fullyQualifiedTypeName = scrutinee.symbolType().fullyQualifiedName();
    return SCRUTINEE_TYPES_FOR_NON_PATTERN_SWITCH.contains(fullyQualifiedTypeName);
  }

  private static @Nullable List<Case> extractCasesFromIfSequence(IfStatementTree topLevelIfStat) {
    var cases = new LinkedList<Case>();
    StatementTree stat;
    for (stat = topLevelIfStat; stat instanceof IfStatementTree ifStat; stat = ifStat.elseStatement()) {
      var caze = convertToCase(ifStat.condition(), ifStat.thenStatement());
      if (caze == null) {
        return null;
      }
      cases.add(caze);
    }
    if (stat != null) {
      cases.add(new DefaultCase(cases.getLast().scrutinee(), stat));
    }
    return cases;
  }

  private static @Nullable Case convertToCase(ExpressionTree condition, StatementTree body) {
    var leftmost = findLeftmostInConjunction(condition);
    var guards = new LinkedList<ExpressionTree>();
    populateGuardsList(condition, guards);
    if (leftmost instanceof PatternInstanceOfTree patInstOf && patInstOf.pattern() != null
      && patInstOf.expression() instanceof IdentifierTree idTree) {
      return new PatternMatchCase(idTree, patInstOf.pattern(), guards, body);
    } else if ((leftmost.kind() == Kind.CONDITIONAL_OR || leftmost.kind() == Kind.EQUAL_TO) && guards.isEmpty()) {
      return buildEqualityCase(leftmost, body);
    } else {
      return null;
    }
  }

  /**
   * Transforms expressions of the form  a == 0 || a == 1 || ...  into an EqualityCase
   */
  private static @Nullable EqualityCase buildEqualityCase(ExpressionTree expr, StatementTree body) {
    var constantsList = new LinkedList<ExpressionTree>();
    IdentifierTree scrutinee = null;
    while (expr.kind() == Kind.CONDITIONAL_OR) {
      var binary = (BinaryExpressionTree) expr;
      var varAndCst = extractVarAndConstFromEqualityCheck(binary.rightOperand());
      if (varAndCst == null) {
        return null;
      } else if (scrutinee == null) {
        scrutinee = varAndCst.a;
      } else if (!varAndCst.a.name().equals(scrutinee.name())) {
        return null;
      }
      constantsList.addFirst(varAndCst.b);
      expr = binary.leftOperand();
    }
    var varAndCst = extractVarAndConstFromEqualityCheck(expr);
    if (varAndCst == null || (scrutinee != null && !varAndCst.a.name().equals(scrutinee.name()))) {
      return null;
    }
    constantsList.addFirst(varAndCst.b);
    return new EqualityCase(scrutinee == null ? varAndCst.a : scrutinee, constantsList, body);
  }

  private static @Nullable Pair<IdentifierTree, ExpressionTree> extractVarAndConstFromEqualityCheck(ExpressionTree expr) {
    if (expr.kind() == Kind.EQUAL_TO) {
      var binary = (BinaryExpressionTree) expr;
      if (binary.leftOperand() instanceof IdentifierTree idTree && isPossibleConstantForCase(binary.rightOperand())) {
        return new Pair<>(idTree, binary.rightOperand());
      } else if (binary.rightOperand() instanceof IdentifierTree idTree && isPossibleConstantForCase(binary.leftOperand())) {
        return new Pair<>(idTree, binary.leftOperand());
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

  private JavaQuickFix computeQuickFix(List<Case> cases, IfStatementTree topLevelIfStat) {
    var canLiftReturn = cases.stream().allMatch(this::canLiftReturn);
    var switchCases = new ArrayList<>();
    for (Case caze : cases) {
      switchCases.add(caze.toCaseGroup(canLiftReturn));
    }
    var scrutinee = cases.get(0).scrutinee();
    SwitchTree switchTree;
    if (canLiftReturn){
      switchTree = (SwitchExpressionTree) expressionTemplate("switch ($0()){}").apply(scrutinee);
    } else {
      switchTree = (SwitchStatementTree) statementTemplate("switch ($0()){}").apply(scrutinee);
    }
    for (Case caze : cases) {
      switchTree.cases().add(caze.toCaseGroup(canLiftReturn));
    }
    var ppsb = new PrettyPrintStringBuilder(FileConfig.DEFAULT_FILE_CONFIG, topLevelIfStat.firstToken(), false);
    switchTree.accept(new Prettyprinter(ppsb));
    var edit = JavaTextEdit.replaceTree(topLevelIfStat, ppsb.toString());
    return JavaQuickFix.newQuickFix(ISSUE_MESSAGE).addTextEdit(edit).build();
  }

  private boolean canLiftReturn(Case caze) {
    var stat = caze.body();
    while (stat instanceof BlockTree block && block.body().size() == 1) {
      stat = block.body().get(0);
    }
    return stat instanceof ReturnStatementTree returnStat && returnStat.expression() != null
      || stat instanceof ThrowStatementTree;
  }

  private static StatementTree liftReturnIfRequested(StatementTree body, boolean lift){
    if (lift && body instanceof ReturnStatementTree returnStat){
      return new ExpressionStatementTreeImpl(returnStat.expression(), null);
    } else {
      return body;
    }
  }

  private sealed interface Case permits PatternMatchCase, EqualityCase, DefaultCase {
    IdentifierTree scrutinee();

    StatementTree body();

    CaseGroupTree toCaseGroup(boolean canLiftReturn);
  }

  private record PatternMatchCase(IdentifierTree scrutinee, PatternTree pattern, List<ExpressionTree> guards,
                                  StatementTree body) implements Case {
    @Override
    public CaseGroupTree toCaseGroup(boolean canLiftReturn) {
      var pat = guards.isEmpty() ? pattern : new GuardedPatternTreeImpl(pattern, token("when"),
        guards.stream().reduce((x, y) -> new BinaryExpressionTreeImpl(Kind.AND, x, token("&&"), y)).get());
      return caseGroupTemplate("case $0() -> $1();").apply(pat, liftReturnIfRequested(body, canLiftReturn));
    }
  }

  private record EqualityCase(IdentifierTree scrutinee, List<ExpressionTree> constants, StatementTree body) implements Case {
    @Override
    public CaseGroupTree toCaseGroup(boolean canLiftReturn) {
      var stats = StatementListTreeImpl.emptyList();
      stats.add(liftReturnIfRequested(body, canLiftReturn));
      return new CaseGroupTreeImpl(
        List.of(new CaseLabelTreeImpl(token("case"), constants, token("->"))),
        stats
      );
    }
  }

  /**
   * For simplicity the default case should have the same scrutinee as the cases before it
   */
  private record DefaultCase(IdentifierTree scrutinee, StatementTree body) implements Case {
    @Override
    public CaseGroupTree toCaseGroup(boolean canLiftReturn) {
      return caseGroupTemplate("default -> $0();").apply(liftReturnIfRequested(body, canLiftReturn));
    }
  }

  private record Pair<A, B>(A a, B b) {
  }

}
