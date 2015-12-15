import org.sonar.java.checks.targets.CustomAnnotation;

@interface MyAnnotation {
  String myName();
  int myInteger() default 0;
  String aaaLast();
}

class MyClass {
  @CustomAnnotation(field1="", field2="", field3="")
  @MyAnnotation(myName="XXX", myInteger=2)
  void goodMethod() {

  }

  @CustomAnnotation(field1="", field3="", field2="")// Noncompliant
  @MyAnnotation(myInteger=2, myName="XXX") // Noncompliant
  @MyAnnotation(myName="XXX", aaaLast = "")
  void wrongMethod() {

  }

  List<@MyAnnotation(myInteger=2, myName="XXX") Object[]> field;// Noncompliant
  List<@MyAnnotation(myName="XXX", aaaLast = "") Object[]> field;
}