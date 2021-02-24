class BusinessClassDelegate implements MySecurityInterface, SecondInterface {

  int aField;

  @MySecurityAnnotation
  public void aMethod() { }

  public void anotherMethod() { } // Noncompliant {{Mandatory Annotation not set @MySecurityAnnotation}}

}
