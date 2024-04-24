package org.sonar.plugins.java.api.lighttree;

public interface LightId extends LightExpr {

  String name();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightId(this);
  }

  @Override
  default Precedence precedence() {
    return Precedence.ATOM;
  }
}
