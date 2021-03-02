package sample;

class AvoidMethodDeclarationRule {

  int aField;

  public void aMethod() { // Noncompliant {{Avoid declaring methods (don't ask why)}}

  }

}
