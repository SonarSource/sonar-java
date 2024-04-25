package org.sonar.plugins.java.api.lighttree;

public interface LightMethodInvocation extends LightExpr, LightStat {

  LightExpr methodSelect();
  LightArguments arguments();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightMethodInvocation(this);
  }

  @Override
  default Precedence precedence() {
    return Precedence.ATOM;
  }

}
