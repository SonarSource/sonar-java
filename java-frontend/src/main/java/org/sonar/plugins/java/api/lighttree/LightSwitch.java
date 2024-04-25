package org.sonar.plugins.java.api.lighttree;

import java.util.List;

public interface LightSwitch extends LightExpr, LightStat {

  LightExpr expression();
  List<? extends LightCaseGroup> cases();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightSwitch(this);
  }

}
