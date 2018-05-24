import org.sonar.java.checks.targets.CustomAnnotation;


@interface MyAnnotation {
  String myName() default "myName";
  int myInteger() default 0;
}
@interface MyAnnotation2 {
  String value() default "defaultValue";
}
@interface MyAnnotation3 {
  int myHexaInteger() default 0x000;
}
class A {
  @CustomAnnotation(field1="field1Default", field2="", field3="") // Noncompliant
  @CustomAnnotation(field1="", field2="field2Default", field3="") // Noncompliant
  @CustomAnnotation(field1="", field2="field2"+"Default", field3="") // compliant : unsupported computation of constants
  @CustomAnnotation(field1="", field2="field2", field3="") // compliant
  @MyAnnotation(myName="myName", myInteger=2) // Noncompliant [[sc=17;ec=32]] {{Remove this default value assigned to parameter "myName".}}
  @MyAnnotation(myName="foo", myInteger=0) // Noncompliant [[sc=31;ec=42]] {{Remove this default value assigned to parameter "myInteger".}}
  @MyAnnotation(myName="foo", myInteger=2)
  @MyAnnotation2("defaultValue") // Noncompliant
  @MyAnnotation2("someValue")
  @MyAnnotation3(myHexaInteger = 0x000) // Noncompliant {{Remove this default value assigned to parameter "myHexaInteger".}}
  @MyUnknownAnnotation("value")
  void goodMethod() {

  }


}
