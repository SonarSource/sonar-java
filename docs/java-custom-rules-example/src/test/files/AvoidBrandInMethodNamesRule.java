/**
 *This file is the sample code against we run our unit test.
 *It is placed src/test/files in order to not be part of the maven compilation.
 **/
class AvoidBrandInNamesCheck {

  int aField;

  public void methodWithMYCOMPANY() { // Noncompliant {{Avoid using Brand in method name}}

  }

  public void methodWithMyCompany() { // Noncompliant {{Avoid using Brand in method name}}

  }

  public void methodWithMyOtherCompany() { // Compliant

  }

}
