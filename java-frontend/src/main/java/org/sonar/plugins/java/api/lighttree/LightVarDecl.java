package org.sonar.plugins.java.api.lighttree;

import javax.annotation.Nullable;

public interface LightVarDecl extends LightStat {

  // modifiers are not supported (yet)

  LightTypeNode type();

  LightId simpleName();

  @Nullable
  LightExpr initializer();

  @Override
  default void accept(LightTreeVisitor visitor) {
    visitor.visitLightVarDecl(this);
  }
}
