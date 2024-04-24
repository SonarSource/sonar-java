package org.sonar.plugins.java.api.lighttree;

public interface LightBinOp extends LightExpr {

  LightExpr leftOperand();
  Operator operator();
  LightExpr rightOperand();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightBinOp(this);
  }

  @Override
  default Precedence precedence() {
    return operator().precedence();
  }
}
