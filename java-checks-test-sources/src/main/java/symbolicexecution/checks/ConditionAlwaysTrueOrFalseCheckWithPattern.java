package symbolicexecution.checks;

public class ConditionAlwaysTrueOrFalseCheckWithPattern {

  public void instanceOfPatternMatching() {
    Object object = new Object();
    // Java 16 pattern matching instance of
    if (object instanceof String s) { // Compliant
    } else if (object instanceof Integer i) { // Compliant
    }

    if (object instanceof String str) {
      // str inherits the contraints from objects
      if (str == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      }
    }
  }

}
