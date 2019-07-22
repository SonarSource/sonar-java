import java.lang.String.*; // Noncompliant
import static java.lang.String.valueOf; // Noncompliant

class A {
  private String disallowed; // Noncompliant [[sc=11;ec=17]]
  private Integer disallowed2; // Noncompliant [[sc=11;ec=18]]
  private java.util.Date date;
  String method1() { // Noncompliant [[sc=3;ec=9]]
  }
void method(String param) { // Noncompliant [[sc=13;ec=19]]
  B var = new B(); // Compliant, no subtypes checked
  String str = new String(); // Noncompliant [[sc=3;ec=9]]
  Object obj =         // Noncompliant [[sc=3;ec=9]]
    String.valueOf(3); // Noncompliant [[sc=5;ec=19]]
  obj = valueOf(3);    // Compliant, already reported in static import
  //System is part of java.lang
  System.out.println(var); // Noncompliant [[sc=3;ec=13]]
  var.doSomething(new Integer(1)); // Noncompliant  [[sc=19;ec=33]]
}
}
class B extends String { // Noncompliant
  public B(Integer a) { // Noncompliant [[sc=12;ec=19]]
  }
  public void doSomething(Integer a) { // Noncompliant [[sc=27;ec=34]]
  }
}
