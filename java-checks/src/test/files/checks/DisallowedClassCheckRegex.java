import java.lang.String.*; // Noncompliant
import static java.lang.String.valueOf; // Noncompliant

class A {
  private String disallowed; // Noncompliant
//        ^^^^^^
  private Integer disallowed2; // Noncompliant
//        ^^^^^^^
  private java.util.Date date;
  String method1() { // Noncompliant
//^^^^^^
  }
void method(String param) { // Noncompliant
//          ^^^^^^
  B var = new B(); // Compliant, no subtypes checked
  String str = new String(); // Noncompliant
//^^^^^^
  Object obj = // Noncompliant
//^^^^^^
    String.valueOf(3); // Noncompliant
//  ^^^^^^^^^^^^^^
  obj = valueOf(3);    // Compliant, already reported in static import
  //System is part of java.lang
  System.out.println(var); // Noncompliant
//^^^^^^^^^^
  var.doSomething(new Integer(1)); // Noncompliant
//                ^^^^^^^^^^^^^^
}
}
class B extends String { // Noncompliant
  public B(Integer a) { // Noncompliant
//         ^^^^^^^
  }
  public void doSomething(Integer a) { // Noncompliant
//                        ^^^^^^^
  }
}
