package org.sonar.plugins.java.api.lighttree;

import java.util.List;

public interface LightBlock extends LightStat {

  List<? extends LightStat> body();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightBlock(this);
  }
}
