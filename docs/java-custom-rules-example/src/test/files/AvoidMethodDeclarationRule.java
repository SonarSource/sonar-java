/**
 *This file is the sample code against we run our unit test.
 *It is placed src/test/files in order to not be part of the maven compilation.
 **/
class AvoidMethodDeclarationCheck {

  int aField;

  public void aMethod() { // Noncompliant {{Avoid declaring methods (don't ask why)}}

  }

}
