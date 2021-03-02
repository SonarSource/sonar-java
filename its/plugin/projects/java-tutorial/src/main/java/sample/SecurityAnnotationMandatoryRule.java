package sample;

class SecurityAnnotationMandatoryRule { }

class BusinessClassDelegate implements MySecurityInterface, SecondInterface {

  int aField;

  @MySecurityAnnotation
  public void aMethod() { }

  @sample.MyOtherSecurityAnnotation
  public void anotherMethod() { } // Noncompliant [[sc=15;ec=28]]  {{Mandatory Annotation not set @MySecurityAnnotation}}

  @VariousAnnotation
  public void differentMethod() { } // Noncompliant [[startColumn=15;endColumn=30]] {{Mandatory Annotation not set @MySecurityAnnotation}}
}

class OtherClass implements FirstInterface, SecondInterface {

  int aField;

  @MySecurityAnnotation
  public void aMethod() { }

  public void anotherMethod() { } // Compliant
}

@interface MySecurityAnnotation { }
@interface VariousAnnotation { }
@interface MyOtherSecurityAnnotation { }

interface FirstInterface { }
interface SecondInterface { }
interface MySecurityInterface { }