class BadMethodName {
  public BadMethodName() {
  }

  void Bad() { // Noncompliant {{Rename this method name to match the regular expression '^[A-Z0-9]*$'.}}
  }

  void good() { // Noncompliant
  }

  @Override
  void BadButOverrides(){
  }

  @Deprecated
  void Bad2() { // Noncompliant
  }

  public String toString() { //Overrides from object
    return "...";
  }
}
