package org.sonar.plugins.java.api.lighttree;

public interface LightExpr extends LightTree {

  default Precedence precedence(){
    // FIXME
    throw new UnsupportedOperationException("not implemented in " + this.getClass());
  }

  default boolean hasPrecedenceOver(LightExpr that){
    return this.precedence().isStrongerThan(that.precedence());
  }

}
