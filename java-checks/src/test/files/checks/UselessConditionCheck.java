public static class Class {

  public void method(Object foo, Object bar) {
    if (foo == bar) { // Compliant
    }
    if (foo == bar && foo == bar) { // Compliant
    }
    if (foo == bar || foo == bar) { // Compliant
    }
    if (foo == bar && foo != bar) { // Noncompliant, always false
    }
    if (foo == bar && foo > bar) { // Noncompliant, always false
    }
    if (foo == bar && foo < bar) { // Noncompliant, always false
    }
    if (foo == bar || foo != bar) { // Noncompliant, always true
    }
  }

}
