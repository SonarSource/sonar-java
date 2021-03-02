package sample;

class AvoidBrandInMethodNamesRule {

  int aField;

  public void methodWithMYCOMPANY() { // Noncompliant {{Avoid using Brand in method name}}

  }

  public void methodWithMyCompany() { // Noncompliant {{Avoid using Brand in method name}}

  }

  public void methodWithMyOtherCompany() { // Compliant

  }

}
