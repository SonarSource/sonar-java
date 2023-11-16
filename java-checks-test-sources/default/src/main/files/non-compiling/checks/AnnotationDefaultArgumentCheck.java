package checks;

import checks.annotations.CustomAnnotation;

@interface MyAnnotationDefaultCheck {
  String myName() default "myName";
  int myInteger() default 0;
}
class AnnotationDefaultArgumentCheck {

  @MyUnknownAnnotation("value")
  void unknownAnnotation() {

  }

  @MyAnnotationDefaultCheck(myName="myName", myInteger=2) // Noncompliant [[sc=29;ec=44]] {{Remove this default value assigned to parameter "myName".}}
  void m1() { }

}
