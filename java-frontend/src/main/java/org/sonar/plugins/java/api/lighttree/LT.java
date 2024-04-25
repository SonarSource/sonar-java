package org.sonar.plugins.java.api.lighttree;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

public final class LT {

  private LT() {
  }

  public record AssignmentExpr(LightExpr variable, LightExpr expression) implements LightAssignExpr {
  }

  public record BinOp(LightExpr leftOperand, Operator operator, LightExpr rightOperand) implements LightBinOp {
  }

  public record Block(List<? extends LightStat> body) implements LightBlock {
    public Block(LightStat... stats) {
      this(Arrays.stream(stats).toList());
    }
  }

  public record Const(Object rawValue) implements LightLiteral {
    public String value() {
      if (rawValue instanceof String s) {
        return "\"" + s + "\"";
      } else if (rawValue instanceof Character c) {
        return "'" + c + "'";
      } else {
        return rawValue.toString();
      }
    }
  }

  public record Id(String name) implements LightId {
  }

  public record IfStat(LightExpr condition, LightStat thenStatement, @Nullable LightStat elseStatement) implements LightIfStat {
  }

  public record VarDecl(LightTypeNode type, LightId simpleName, LightExpr initializer) implements LightVarDecl {
  }

  public record TypeNode(String typeName) implements LightTypeNode {
  }

  public record Args(List<LightExpr> args) implements LightArguments {
    public Args(LightExpr... args){
      this(Arrays.stream(args).toList());
    }
  }

  public record Invocation(LightExpr methodSelect, LightArguments arguments) implements LightMethodInvocation {
  }

  public record Switch(LightExpr expression, List<LightCaseGroup> cases) implements LightSwitch {
    public Switch(LightExpr expression, LightCaseGroup... cases) {
      this(expression, Arrays.asList(cases));
    }
  }

  public record CaseGroup(List<LightCaseLabel> labels, LightStat bodyAsStat) implements LightCaseGroup {
    public CaseGroup(LightCaseLabel label, LightStat body){
      this(List.of(label), body);
    }
  }

  public record CaseLabel(List<LightExpr> expressions) implements LightCaseLabel {
    public CaseLabel(LightExpr... expressions){
      this(Arrays.asList(expressions));
    }
  }

}
