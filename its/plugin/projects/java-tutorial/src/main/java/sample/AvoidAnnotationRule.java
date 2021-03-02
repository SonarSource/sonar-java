package sample;

class AvoidAnnotationRule {

  int aField;

  @MyAnnotation
  public void aMethod() { }

  @Zuper // Noncompliant {{Avoid using annotation @Zuper}}
  public void anotherMethod() { }

  @sample.Zuper // Compliant
  public void aThirdMethod() { }

}

@interface Zuper { }
@interface MyAnnotation { }
