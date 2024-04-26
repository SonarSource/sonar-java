package org.sonar.java.checks.prettyprint;

import org.sonar.plugins.java.api.tree.Tree;

public final class Associativity {

  private Associativity(){
  }

  public static boolean isKnownAssociativeOperator(Tree.Kind operatorKind){
    return switch (operatorKind){
      case PLUS, MULTIPLY, CONDITIONAL_AND, CONDITIONAL_OR -> true;
      default -> false;
    };
  }

}
