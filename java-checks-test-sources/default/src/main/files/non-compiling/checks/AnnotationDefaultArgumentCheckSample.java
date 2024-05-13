package checks;

import checks.annotations.CustomAnnotation;

@interface MyAnnotationDefaultCheck {
  String myName() default "myName";
  int myInteger() default 0;
}
class AnnotationDefaultArgumentCheckSample {

  @MyUnknownAnnotation("value")
  void unknownAnnotation() {

  }

  @MyAnnotationDefaultCheck(myName="myName", myInteger=2) // Noncompliant {{Remove this default value assigned to parameter "myName".}}
//                          ^^^^^^^^^^^^^^^
  void m1() { }

}
