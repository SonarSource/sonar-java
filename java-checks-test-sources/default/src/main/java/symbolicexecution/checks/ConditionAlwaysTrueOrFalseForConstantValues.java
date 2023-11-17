package symbolicexecution.checks;

public class ConditionAlwaysTrueOrFalseForConstantValues {

  interface I {
    int i = 0;
  }

  class Impl implements I {
    void alwaysTrue() {
      if (i == 0) { // Noncompliant
        System.out.println("Always true");
      }
    }
    
    boolean returnsAlwaysTrue() {
      return i == 0; // FN 
    }
    
  }

  static final int constant = 0;

  void alwaysTrue() {
    // Fixed, now ZERO constraint correctly applies to identifiers out of method scope
    if (constant == 0) { // Noncompliant
      System.out.println("Always true");
    } 
  }

}
