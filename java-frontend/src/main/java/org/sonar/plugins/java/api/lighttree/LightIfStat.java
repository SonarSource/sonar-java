package org.sonar.plugins.java.api.lighttree;

import javax.annotation.Nullable;

public interface LightIfStat extends LightTree {

  LightExpr condition();

  LightStat thenStatement();

  @Nullable
  LightStat elseStatement();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightIfStat(this);
  }
}
