package org.sonar.java.checks.quickfixes;


import java.util.List;
import java.util.Optional;

public sealed interface Ast {

  void accept(Visitor visitor);

  sealed interface Statement extends Ast {
    boolean requiresSemicolon();
  }

  sealed interface ElseBranchStat extends Statement {
  }

  sealed interface Expression extends Ast {
    Precedence precedence();

    default BinaryOperator plus(Expression rhs){
      return new BinaryOperator(this, Operator.BinaryOperator.ADD, rhs);
    }
    default BinaryOperator minus(Expression rhs){
      return new BinaryOperator(this, Operator.BinaryOperator.SUB, rhs);
    }
    default BinaryOperator times(Expression rhs){
      return new BinaryOperator(this, Operator.BinaryOperator.MUL, rhs);
    }
    default BinaryOperator eq(Expression rhs){
      return new BinaryOperator(this, Operator.BinaryOperator.EQUALITY, rhs);
    }
    // TODO other binary operators
  }

  sealed interface LValue extends Expression {
    default Assignment assig(Expression rhs){
      return new Assignment(this, Operator.AssignmentOperator.ASSIG, rhs);
    }
    // TODO other assignment operators
  }

  record VarDecl(String typeOrVar, String varName, Optional<Expression> initializerExpr) implements Statement {
    @Override
    public boolean requiresSemicolon() {
      return true;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitVarDecl(this);
    }
  }

  record IfStat(Expression condition, Block thenBr, Optional<ElseBranchStat> elseBr) implements ElseBranchStat {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitIfStat(this);
    }

    @Override
    public boolean requiresSemicolon() {
      return false;
    }
  }

  record Switch(Expression scrutinee, List<Case> cases) implements Statement {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitSwitch(this);
    }

    @Override
    public boolean requiresSemicolon() {
      return false;
    }
  }

  record Case(Optional<Pattern> pattern, Ast body) implements Ast {
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

  record GuardedPattern(GuardablePattern pattern, Expression guardExpr) implements Pattern {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitGuardablePattern(this);
    }
  }

  sealed interface GuardablePattern extends Pattern {
    default GuardedPattern Where(Expression guard){
      return new GuardedPattern(this, guard);
    }
  }

  record VariablePattern(String typeOrVar, String varName) implements GuardablePattern {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitVariablePattern(this);
    }
  }

  record RecordPattern(String recordName, List<String> typeVars, List<Pattern> fields) implements GuardablePattern {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitRecordPattern(this);
    }
  }

  record Block(List<Statement> statements) implements ElseBranchStat {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitBlock(this);
    }

    @Override
    public boolean requiresSemicolon() {
      return false;
    }
  }

  record BinaryOperator(Expression lhs, Operator.BinaryOperator operator, Expression rhs) implements Expression {
    @Override
    public Precedence precedence() {
      return operator.precedence();
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitBinop(this);
    }
  }

  record Assignment(LValue lValue, Operator.AssignmentOperator assigOp, Expression newValue) implements Statement, Expression {
    @Override
    public Precedence precedence() {
      return assigOp.precedence();
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitAssignment(this);
    }

    @Override
    public boolean requiresSemicolon() {
      return true;
    }
  }

  record Const(Object value) implements Expression {
    @Override
    public Precedence precedence() {
      return Precedence.ATOM;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitConst(this);
    }
  }

  record HardCodedStat(String code) implements Statement {
    @Override
    public void accept(Visitor visitor) {
      visitor.visitHardCodedStat(this);
    }

    @Override
    public boolean requiresSemicolon() {
      return true;
    }
  }

  record HardCodedExpr(String code) implements LValue {

    @Override
    public Precedence precedence() {
      return Precedence.ATOM;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visitHardCodedExpr(this);
    }
  }

  interface Visitor {

    void visitVarDecl(VarDecl varDecl);

    void visitIfStat(IfStat ifStat);

    void visitBlock(Block block);

    void visitHardCodedStat(HardCodedStat stat);

    void visitHardCodedExpr(HardCodedExpr expr);

    void visitSwitch(Switch swtch);

    void visitCase(Case caze);

    void visitValuePattern(ValuePattern valuePattern);

    void visitGuardablePattern(GuardedPattern guardedPattern);

    void visitVariablePattern(VariablePattern variablePattern);

    void visitRecordPattern(RecordPattern recordPattern);

    void visitBinop(BinaryOperator binop);

    void visitAssignment(Assignment assignment);

    void visitConst(Const cst);

  }

}
