package org.sonar.java.checks.quickfixes;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.lighttree.LightAssignExpr;
import org.sonar.plugins.java.api.lighttree.LightBinOp;
import org.sonar.plugins.java.api.lighttree.LightBlock;
import org.sonar.plugins.java.api.lighttree.LightExpr;
import org.sonar.plugins.java.api.lighttree.LightId;
import org.sonar.plugins.java.api.lighttree.LightIfStat;
import org.sonar.plugins.java.api.lighttree.LightLiteral;
import org.sonar.plugins.java.api.lighttree.LightStat;
import org.sonar.plugins.java.api.lighttree.LightTypeNode;
import org.sonar.plugins.java.api.lighttree.LightVarDecl;
import org.sonar.plugins.java.api.lighttree.Operator;
import org.sonar.plugins.java.api.semantic.Type;

public final class P {

  private P() {
  }

  public record AssignmentExpr(LightExpr variable, LightExpr expression) implements LightAssignExpr {
  }
  
  public record BinOp(LightExpr leftOperand, Operator operator, LightExpr rightOperand) implements LightBinOp {
  }

  public record Block(List<LightStat> body) implements LightBlock {
    public Block(LightStat... stats){
      this(Arrays.stream(stats).toList());
    }
  }

  public record Const(Object rawValue) implements LightLiteral {
    public String value(){
      if (rawValue instanceof String s){
        return "\"" + s + "\"";
      } else if (rawValue instanceof Character c){
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

}
