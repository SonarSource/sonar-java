package org.sonar.plugins.java.api.lighttree;

public interface LightTypeNode extends LightTree {

  String typeName();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightTypeNode(this);
  }
}
