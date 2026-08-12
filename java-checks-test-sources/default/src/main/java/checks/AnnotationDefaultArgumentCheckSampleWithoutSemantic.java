package checks;

import checks.annotations.CustomAnnotation;

@interface MyAnnotationDefaultCheckWS {
  String myName() default "myName";
  int myInteger() default 0;
}
@interface MyAnnotationDefaultCheck2WS {
  String value() default "defaultValue";
}
@interface MyAnnotationDefaultCheck3WS {
  int myHexaInteger() default 0x000;
}
class AnnotationDefaultArgumentCheckSampleWithoutSemantic {
  private static final String FIELD_VALUE = "field1Default";
  private static final int FIELD_VALUE_INT = 0;

  @MyAnnotationDefaultCheckWS(myName="myName", myInteger=2) // Noncompliant

  @MyAnnotationDefaultCheck2WS("defaultValue") // Noncompliant
  @MyAnnotationDefaultCheck3WS(myHexaInteger = 0x000) // Noncompliant
  void m1() { }

  @MyAnnotationDefaultCheckWS(myName="foo", myInteger=0) // Noncompliant

  @MyAnnotationDefaultCheck2WS("someValue")
  void m2() { }

  @MyAnnotationDefaultCheckWS(myName="foo", myInteger=2)
  void m3() { }

  @MyAnnotationDefaultCheckWS(myName="foo", myInteger=FIELD_VALUE_INT) // Noncompliant
  void m4() { }

  @CustomAnnotation(field1="", field2="field2", field3="") // compliant
  void m5() { }

}
