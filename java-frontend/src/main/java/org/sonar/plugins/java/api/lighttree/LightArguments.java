package org.sonar.plugins.java.api.lighttree;

import java.util.List;

public interface LightArguments extends LightTree {

  List<? extends LightExpr> args();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightArguments(this);
  }

}
