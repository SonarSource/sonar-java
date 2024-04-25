package org.sonar.plugins.java.api.lighttree;

import java.util.List;

public interface LightCaseGroup extends LightTree {

  List<? extends LightCaseLabel> labels();
  LightStat bodyAsStat();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightCaseGroup(this);
  }

}
