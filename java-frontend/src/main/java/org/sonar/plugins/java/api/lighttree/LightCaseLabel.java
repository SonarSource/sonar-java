package org.sonar.plugins.java.api.lighttree;

import java.util.List;

public interface LightCaseLabel extends LightTree {

  List<? extends LightExpr> expressions();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightCaseLabel(this);
  }
}
