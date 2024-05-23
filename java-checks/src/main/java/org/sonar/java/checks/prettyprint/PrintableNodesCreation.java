package org.sonar.java.checks.prettyprint;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class PrintableNodesCreation {

  private PrintableNodesCreation(){}

  // Tokens may be set to null only if they are not used by the prettyprinter

  // <editor-fold desc="Expressions">

  public static BinaryExpressionTreeImpl binop(ExpressionTree lhs, Tree.Kind operator, ExpressionTree rhs) {
    return new BinaryExpressionTreeImpl(operator, lhs, token(KindsPrinter.printExprKind(operator)), rhs);
  }

  // </editor-fold>

  // <editor-fold desc="Statements">

  public static BlockTree block(StatementTree... stats) {
    return new BlockTreeImpl(null, Arrays.stream(stats).toList(), null);
  }

  public static ReturnStatementTree returnStat(){
    return new ReturnStatementTreeImpl(null, null, null);
  }

  public static ReturnStatementTree returnStat(ExpressionTree expr){
    return new ReturnStatementTreeImpl(null, expr, null);
  }

  // </editor-fold>

  // <editor-fold desc="Switches">

  public static SwitchStatementTree switchStat(ExpressionTree scrutinee, List<CaseGroupTreeImpl> cases) {
    return new SwitchStatementTreeImpl(null, null, scrutinee, null, null, cases, null);
  }

  public static SwitchExpressionTree switchExpr(ExpressionTree scrutinee, List<CaseGroupTreeImpl> cases){
    return new SwitchExpressionTreeImpl(null, null, scrutinee, null, null, cases, null);
  }

  public static CaseGroupTreeImpl switchCaseFromLabels(List<CaseLabelTreeImpl> caseLabelTrees, StatementTree body){
    return new CaseGroupTreeImpl(caseLabelTrees, makeStatementsList(body));
  }

  public static CaseGroupTreeImpl switchCase(PatternTree patternTree, List<ExpressionTree> guards, StatementTree body) {
    return switchCaseFromLabels(List.of(new CaseLabelTreeImpl(null, List.of(withGuards(patternTree, guards)), token("->"))), body);
  }

  public static CaseGroupTreeImpl switchCase(List<ExpressionTree> constants, StatementTree body) {
    return switchCaseFromLabels(List.of(new CaseLabelTreeImpl(null, constants, token("->"))), body);
  }

  public static CaseGroupTreeImpl switchDefault(StatementTree body) {
    var label = new CaseLabelTreeImpl(null, List.of(), token("->"));
    return new CaseGroupTreeImpl(List.of(label), makeStatementsList(body));
  }

  private static PatternTree withGuards(PatternTree unguardedPattern, List<ExpressionTree> guards) {
    return guards.isEmpty() ? unguardedPattern
      : new GuardedPatternTreeImpl(unguardedPattern, null, guards.stream().reduce((x, y) -> binop(x, Tree.Kind.CONDITIONAL_AND, y)).get()
    );
  }

  // </editor-fold>

  // <editor-fold desc="Auxiliary methods">

  private static StatementListTreeImpl makeStatementsList(StatementTree body) {
    var ls = StatementListTreeImpl.emptyList();
    ls.add(body);
    return ls;
  }

  private static InternalSyntaxToken token(String s) {
    return new InternalSyntaxToken(0, 0, s, List.of(), false);
  }

  // </editor-fold>

}
