package org.sonar.java.checks.quickfixes;

import java.util.List;
import java.util.function.Consumer;
import org.sonar.java.checks.quickfixes.Ast.HardCodedExpr;
import org.sonar.java.checks.quickfixes.Ast.HardCodedStat;

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
    printAllWithSep(block.statements(), PrettyprintStringBuilder::newLine);
    pps.decIndent().newLine().add("}");
  }

  @Override
  public void visitHardCodedStat(HardCodedStat stat) {
    String statCode = stat.code().trim();
    pps.add(statCode);
    if (!statCode.endsWith(";")) {
      pps.add(";");
    }
  }

  @Override
  public void visitExpression(HardCodedExpr expr) {
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
    pps.add("case ");
    caze.pattern().accept(this);
    pps.addSpace();
    caze.guardExpr().ifPresent(guardExpr -> {
      pps.add(" when ");
      guardExpr.accept(this);
    });
    pps.add(" -> ");
    caze.body().accept(this);
  }

  @Override
  public void visitValuePattern(Ast.ValuePattern valuePattern) {
    pps.add(valuePattern.value());
  }

  @Override
  public void visitVariablePattern(Ast.VariablePattern variablePattern) {
    pps.add(variablePattern.typeOrVar()).addSpace().add(variablePattern.varName());
  }

  @Override
  public void visitRecordPattern(Ast.RecordPattern recordPattern) {
    pps.add(recordPattern.recordName());
    if (!recordPattern.typeVars().isEmpty()){
      pps.add("<").add(String.join(", ", recordPattern.typeVars())).add(">");
    }
    pps.add("(");
    printAllWithSep(recordPattern.fields(), PrettyprintStringBuilder::addComma);
    pps.add(")");
  }

  private void printAllWithSep(List<? extends Ast> asts, Consumer<PrettyprintStringBuilder> sep){
    var iter = asts.iterator();
    while (iter.hasNext()){
      iter.next().accept(this);
      if (iter.hasNext()){
        sep.accept(pps);
      }
    }
  }

}
