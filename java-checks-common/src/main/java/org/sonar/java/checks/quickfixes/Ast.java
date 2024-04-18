package org.sonar.java.checks.quickfixes;


import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public sealed interface Ast {

  void accept(Visitor visitor);

  sealed interface Statement extends Ast {
  }

  sealed interface ElseBranchStat extends Statement {
  }

  sealed interface Expression extends Ast {
  }

  record IfStat(Expression condition, Block thenBr, Optional<ElseBranchStat> elseBr) implements ElseBranchStat {

    public IfStat(Expression condition, Block thenBr) {
      this(condition, thenBr, Optional.empty());
    }

    public IfStat(Expression condition, Block thenBr, ElseBranchStat elseBr) {
      this(condition, thenBr, Optional.of(elseBr));
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitIfStat(this);
    }
  }

  record Switch(Expression scrutinee, List<Case> cases) implements Statement {

    public Switch(Expression scrutinee, Case... cases){
      this(scrutinee, Arrays.asList(cases));
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitSwitch(this);
    }
  }

  record Case(Pattern pattern, Optional<Expression> guardExpr, Ast body) implements Ast {

    public Case(Pattern pattern, Ast body){
      this(pattern, Optional.empty(), body);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitCase(this);
    }
  }

  sealed interface Pattern extends Ast {
  }

  record ValuePattern(String value) implements Pattern {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitValuePattern(this);
    }
  }

  record VariablePattern(String typeOrVar, String varName) implements Pattern {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitVariablePattern(this);
    }
  }

  record RecordPattern(String recordName, List<String> typeVars, List<Pattern> fields) implements Pattern {

    public RecordPattern(String recordName, Pattern... fields){
      this(recordName, List.of(), Arrays.asList(fields));
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitRecordPattern(this);
    }
  }

  record Block(List<Statement> statements) implements ElseBranchStat {

    public Block(Statement... statements) {
      this(Arrays.asList(statements));
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitBlock(this);
    }
  }

  record HardCodedStat(String code) implements Statement {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitHardCodedStat(this);
    }
  }

  record HardCodedExpr(String code) implements Expression {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitExpression(this);
    }
  }

  interface Visitor {

    void visitIfStat(IfStat ifStat);
    void visitBlock(Block block);
    void visitHardCodedStat(HardCodedStat stat);
    void visitExpression(HardCodedExpr expr);
    void visitSwitch(Switch swtch);
    void visitCase(Case caze);
    void visitValuePattern(ValuePattern valuePattern);
    void visitVariablePattern(VariablePattern variablePattern);
    void visitRecordPattern(RecordPattern recordPattern);

  }

}
