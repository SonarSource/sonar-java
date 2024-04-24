package org.sonar.plugins.java.api.lighttree;

public interface LightAssignExpr extends LightExpr, LightStat {

  LightExpr variable();
  LightExpr expression();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightAssignExpr(this);
  }

  @Override
  default Precedence precedence() {
    return Precedence.ASSIGNMENT;
  }
}
