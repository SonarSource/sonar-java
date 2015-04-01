public static class Class {

  public void method(Object foo, Object bar, boolean condition) {
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
    if (condition && !condition) { // Noncompliant, always false
    }
    if (condition || !condition) { // Noncompliant, always true
    }
    if ((foo == bar || condition) && !(foo == bar || condition)) { // Noncompliant, always false
    }
    if ((foo == bar || condition) || !(foo == bar || condition)) { // Noncompliant, always true
    }
    if (!(foo == bar || condition) && (foo == bar || condition)) { // Noncompliant, always false
    }
    if (!(foo == bar || condition) || (foo == bar || condition)) { // Noncompliant, always true
    }
  }

}
