package org.sonar.plugins.java.api.lighttree;

public interface LightTree {

  default void accept(LightTreeVisitor visitor){
    // FIXME
    throw new UnsupportedOperationException("not implemented on " + this.getClass());
  }

}
