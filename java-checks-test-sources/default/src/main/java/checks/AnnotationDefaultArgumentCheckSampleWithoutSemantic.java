package checks;

import checks.annotations.CustomAnnotation;

@interface MyAnnotationDefaultCheck {
  String myName() default "myName";
  int myInteger() default 0;
}
@interface MyAnnotationDefaultCheck2 {
  String value() default "defaultValue";
}
@interface MyAnnotationDefaultCheck3 {
  int myHexaInteger() default 0x000;
}
class AnnotationDefaultArgumentCheckSampleWithoutSemantic {
  private static final String FIELD_VALUE = "field1Default";
  private static final int FIELD_VALUE_INT = 0;

  @CustomAnnotation(field1="field1Default", field2="", field3="") // FN
  @MyAnnotationDefaultCheck(myName="myName", myInteger=2) // Noncompliant

  @MyAnnotationDefaultCheck2("defaultValue") // Noncompliant
  @MyAnnotationDefaultCheck3(myHexaInteger = 0x000) // Noncompliant
  void m1() { }

  @MyAnnotationDefaultCheck(myName="foo", myInteger=0) // Noncompliant

  @MyAnnotationDefaultCheck2("someValue")
  @CustomAnnotation(field1="", field2="field2Default", field3="") // FN
  void m2() { }

  @MyAnnotationDefaultCheck(myName="foo", myInteger=2)
  @CustomAnnotation(field1="", field2="field2"+"Default", field3="") // FN
  void m3() { }

  @CustomAnnotation(field1=FIELD_VALUE, field2="", field3="") // FN
  @MyAnnotationDefaultCheck(myName="foo", myInteger=FIELD_VALUE_INT) // Noncompliant
  void m4() { }

  @CustomAnnotation(field1="", field2="field2", field3="") // compliant
  void m5() { }

}
