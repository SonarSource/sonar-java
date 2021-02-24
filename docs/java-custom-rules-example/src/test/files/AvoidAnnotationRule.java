/**
 *This file is the sample code against we run our unit test.
 *It is placed src/test/files in order to not be part of the maven compilation.
 **/
class AvoidAnnotationRule {

  int aField;

  @MyAnnotation
  public void aMethod() { }

  @Zuper // Noncompliant {{Avoid using annotation @Zuper}}
  public void anotherMethod() { }

  @other.Zuper // Compliant
  public void aThirdMethod() { }

}
