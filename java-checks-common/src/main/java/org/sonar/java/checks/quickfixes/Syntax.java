package org.sonar.java.checks.quickfixes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.sonar.java.checks.quickfixes.Ast.*;

public final class Syntax {
  private Syntax(){
  }

  public static IfStat If(Expression condition, Block thenBr){
    return new IfStat(condition, thenBr, Optional.empty());
  }

  public static IfStat If(Expression condition, Block thenBr, ElseBranchStat elseBr){
    return new IfStat(condition, thenBr, Optional.of(elseBr));
  }

  public static Switch Switch(Expression scrutinee, Case... cases){
    return new Switch(scrutinee, Arrays.asList(cases));
  }

  public static Case Case(Pattern pattern, Ast body){
    return new Case(pattern, body);
  }

  public static ValuePattern Pat(String value){
    return new ValuePattern(value);
  }

  public static DefaultPattern defaultPat(){
    return new DefaultPattern();
  }

  public static VariablePattern Pat(String typeOrVar, String varName){
    return new VariablePattern(typeOrVar, varName);
  }

  public static RecordPattern Pat(String recordName, Pattern... fields){
    return new RecordPattern(recordName, List.of(), Arrays.asList(fields));
  }

  public static RecordPattern Pat(String recordName, List<String> typeVars, Pattern... fields){
    return new RecordPattern(recordName, typeVars, Arrays.asList(fields));
  }

  public static Block Block(Statement... stats){
    return new Block(Arrays.asList(stats));
  }

  public static Const cst(Object cst){
    return new Const(cst);
  }

  public static HardCodedStat stat(String code){
    return new HardCodedStat(code);
  }

  public static HardCodedExpr expr(String code){
    return new HardCodedExpr(code);
  }

}
