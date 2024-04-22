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

package org.sonar.java.checks.quickfixes;

import java.util.List;
import java.util.function.Consumer;
import org.sonar.java.checks.quickfixes.Ast.HardCodedBlock;
import org.sonar.java.checks.quickfixes.Ast.Expression;
import org.sonar.java.checks.quickfixes.Ast.HardCodedExpr;
import org.sonar.java.checks.quickfixes.Ast.HardCodedStat;
import org.sonar.java.checks.quickfixes.Ast.Statement;

import static org.sonar.java.checks.quickfixes.Ast.Block;
import static org.sonar.java.checks.quickfixes.Ast.IfStat;
import static org.sonar.java.checks.quickfixes.Ast.Visitor;

public final class Prettyprinter implements Visitor {
  private final PrettyprintStringBuilder pps;

  public static String prettyprint(Ast ast, FileConfig fileConfig) {
    PrettyprintStringBuilder pps = new PrettyprintStringBuilder(fileConfig);
    ast.accept(new Prettyprinter(pps));
    return pps.toString();
  }

  public Prettyprinter(PrettyprintStringBuilder pps) {
    this.pps = pps;
  }

  @Override
  public void visitVarDecl(Ast.VarDecl varDecl) {
    pps.add(varDecl.typeOrVar()).addSpace().add(varDecl.varName());
    varDecl.initializerExpr().ifPresent(initExpr -> {
      pps.add(" = ");
      initExpr.accept(this);
    });
  }

  @Override
  public void visitIfStat(IfStat ifStat) {
    pps.add("if (");
    ifStat.condition().accept(this);
    pps.add(") ");
    ifStat.thenBr().accept(this);
    var elseBrOpt = ifStat.elseBr();
    elseBrOpt.ifPresent(elseBr -> {
      pps.add(" else ");
      elseBr.accept(this);
    });
  }

  @Override
  public void visitBlock(Block block) {
    pps.add("{").incIndent().newLine();
    printStatsSeq(block.statements());
    pps.decIndent().newLine().add("}");
  }

  @Override
  public void visitHardCodedStat(HardCodedStat stat) {
    pps.add(stat.code().trim());
  }

  @Override
  public void visitHardCodedExpr(HardCodedExpr expr) {
    pps.add(expr.code().trim());
  }

  @Override
  public void visitSwitch(Ast.Switch swtch) {
    pps.add("switch (");
    swtch.scrutinee().accept(this);
    pps.add(") {").incIndent().newLine();
    printAllWithSep(swtch.cases(), PrettyprintStringBuilder::newLineIfNotEmpty);
    pps.decIndent().newLine().add("}");
  }

  @Override
  public void visitCase(Ast.Case caze) {
    caze.pattern().ifPresentOrElse(pattern -> {
      pps.add("case ");
      pattern.accept(this);
    }, () -> {
      pps.add("default");
    });
    pps.add(" -> ");
    caze.body().accept(this);
  }

  @Override
  public void visitValuePattern(Ast.ValuePattern valuePattern) {
    pps.add(valuePattern.value());
  }

  @Override
  public void visitGuardablePattern(Ast.GuardedPattern guardedPattern) {
    guardedPattern.pattern().accept(this);
    pps.add(" when ");
    guardedPattern.guardExpr().accept(this);
  }

  @Override
  public void visitVariablePattern(Ast.VariablePattern variablePattern) {
    pps.add(variablePattern.typeOrVar()).addSpace().add(variablePattern.varName());
  }

  @Override
  public void visitRecordPattern(Ast.RecordPattern recordPattern) {
    pps.add(recordPattern.recordName());
    if (!recordPattern.typeVars().isEmpty()) {
      pps.add("<").add(String.join(", ", recordPattern.typeVars())).add(">");
    }
    pps.add("(");
    printAllWithSep(recordPattern.fields(), PrettyprintStringBuilder::addComma);
    pps.add(")");
  }

  @Override
  public void visitBinop(Ast.BinaryOperator binop) {
    var parenthesizeLhs = binop.hasPrecedenceOver(binop.lhs());
    maybeParenthesize(binop.lhs(), parenthesizeLhs);
    pps.addSpace().add(binop.operator().code()).addSpace();
    var parenthesizeRhs = !binop.rhs().hasPrecedenceOver(binop) && !isSameAssociativeOperator(binop.rhs(), binop.operator());
    maybeParenthesize(binop.rhs(), parenthesizeRhs);
  }

  @Override
  public void visitAssignment(Ast.Assignment assignment) {
    assignment.lValue().accept(this);
    pps.addSpace().add(assignment.assigOp().code()).addSpace();
    assignment.newValue().accept(this);
  }

  @Override
  public void visitConst(Ast.Const cst) {
    var rawString = cst.value().toString();
    pps.add(cst.value() instanceof String ? ("\"" + rawString + "\"") : rawString);
  }

  @Override
  public void visitHardCodedBlock(HardCodedBlock hardCodedBlock) {
    var code = hardCodedBlock.code();
    var lines = code.lines().toList();
    var lastLine = lines.get(lines.size() - 1);
    var baseIndentLevel = countLeadingIndents(lastLine);
    pps.add(code.indent(-baseIndentLevel * pps.fileConfig().indent().length()));
  }

  private int countLeadingIndents(String str) {
    var indent = pps.fileConfig().indent();
    var cnt = 0;
    while (str.startsWith(indent, cnt * indent.length())) {
      cnt += 1;
    }
    return cnt;
  }

  private <T extends Ast> void printAllWithSep(List<T> asts, Consumer<PrettyprintStringBuilder> sep) {
    var iter = asts.iterator();
    while (iter.hasNext()) {
      iter.next().accept(this);
      if (iter.hasNext()) {
        sep.accept(pps);
      }
    }
  }

  private static boolean isSameAssociativeOperator(Expression subExpr, Operator topLevelOp) {
    return subExpr instanceof Ast.BinaryOperator subBinop && subBinop.operator() == topLevelOp && topLevelOp.isAssociative();
  }

  private void printStatsSeq(List<Statement> stats) {
    var iter = stats.iterator();
    while (iter.hasNext()) {
      var stat = iter.next();
      stat.accept(this);
      if (stat.requiresSemicolon() && !pps.endsWith(';')) {
        pps.add(";");
      }
      if (iter.hasNext()) {
        pps.newLine();
      }
    }
  }

  private void maybeParenthesize(Expression expr, boolean parenthesize) {
    if (parenthesize) {
      pps.add("(");
    }
    expr.accept(this);
    if (parenthesize) {
      pps.add(")");
    }
  }

}
