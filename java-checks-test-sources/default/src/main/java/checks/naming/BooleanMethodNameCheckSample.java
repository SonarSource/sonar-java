package checks.naming;

class BooleanMethodNameCheckSample {
  boolean foo() { return true; } // Compliant
  boolean getFoo() { return true; } // Noncompliant {{Rename this method to start with "is" or "has".}}
  boolean isFoo() { return true; } // Compliant
  boolean hasFoo() { return true; } // Compliant

  boolean getBooleanBecauseIWantTo() { return true; } // Compliant

  int bar() { return 0; }
}

class BooleanMethodNameCheckSampleB extends BooleanMethodNameCheckSample implements BooleanMethodNameCheckSampleI {
  @Override
  boolean foo() { return false; } // Compliant - overrides are not taken into account
  public boolean getOk() { return false; } // Compliant - overrides are not taken into account (even without the annotation)
}

interface BooleanMethodNameCheckSampleI {
  boolean getOk(); // Noncompliant
}
