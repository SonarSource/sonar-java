import org.sonar.java.checks.targets.CustomAnnotation;
import org.sonar.java.MyUnknownAnnotation;

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

  @CustomAnnotation(field1="", field3="", field2="") // Noncompliant {{Reorder annotation arguments to match the order of declaration.}}
  @MyAnnotation(myInteger=2, myName="XXX") // Noncompliant
  @MyAnnotation(myName="XXX", aaaLast = "")
  void wrongMethod() {

  }

  List<@MyAnnotation(myInteger=2, myName="XXX") Object[]> field; // Noncompliant [[sc=9;ec=21]]
  List<@MyAnnotation(myName="XXX", aaaLast = "") Object[]> field2;
}

@MyUnknownAnnotation(name = "XXX") // Compliant
class MySecondClass {}
