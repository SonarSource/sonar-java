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
class AnnotationDefaultArgumentCheck {
  @CustomAnnotation(field1="field1Default", field2="", field3="") // Noncompliant
  @MyAnnotationDefaultCheck(myName="myName", myInteger=2) // Noncompliant [[sc=29;ec=44]] {{Remove this default value assigned to parameter "myName".}}
  @MyAnnotationDefaultCheck2("defaultValue") // Noncompliant
  @MyAnnotationDefaultCheck3(myHexaInteger = 0x000) // Noncompliant {{Remove this default value assigned to parameter "myHexaInteger".}}
  void m1() { }

  @MyAnnotationDefaultCheck(myName="foo", myInteger=0) // Noncompliant [[sc=43;ec=54]] {{Remove this default value assigned to parameter "myInteger".}}
  @MyAnnotationDefaultCheck2("someValue")
  @CustomAnnotation(field1="", field2="field2Default", field3="") // Noncompliant
  void m2() { }


  @MyAnnotationDefaultCheck(myName="foo", myInteger=2)
  @CustomAnnotation(field1="", field2="field2"+"Default", field3="") // compliant : unsupported computation of constants
  void m3() { }

  @CustomAnnotation(field1="", field2="field2", field3="") // compliant
  void m5() { }

}
