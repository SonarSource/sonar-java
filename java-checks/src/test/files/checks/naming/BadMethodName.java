class BadMethodName {
  public BadMethodName() {
  }

  void Bad() { // Noncompliant [[sc=8;ec=11]] {{Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
  }

  void good() {
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

  @AttributeDefinition(
    name = "Sling resource types",
    description = "Sling Resource Type to bind the downloads to.")
  String[] sling_servlet_resourceTypes(); // Compliant
}
