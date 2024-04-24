package org.sonar.plugins.java.api.lighttree;

public interface LightLiteral extends LightExpr {

  String value();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightLiteral(this);
  }

  @Override
  default Precedence precedence() {
    return Precedence.ATOM;
  }
}
