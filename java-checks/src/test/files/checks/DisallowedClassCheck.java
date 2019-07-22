import unknown;
import java.lang.Object;
import java.lang.String; // Noncompliant
import java.lang.String.*; // Noncompliant
import static java.lang.String.valueOf; // Noncompliant
import static java.lang.String.CASE_INSENSITIVE_ORDER; // Noncompliant

class A {
  private String disallowed; // Noncompliant [[sc=11;ec=17]]
  private Integer allowed;
  private java.util.Date date;
  String method1() { // Noncompliant [[sc=3;ec=9]]
  }
  Unknown method(String param) { // Noncompliant [[sc=18;ec=24]]
    B var = new B(); // Compliant, no subtypes checked
    String str = new String(); // Noncompliant [[sc=5;ec=11]]
    Object obj1 = String.valueOf(3); // Noncompliant [[sc=19;ec=33]]
    Object obj2 = valueOf(3); // Compliant, already reported in the static import
    Object obj3 = unknown(3);
    Object obj4 = java.lang.String.CASE_INSENSITIVE_ORDER; // Noncompliant [[sc=19;ec=58]]
    Object obj5 = String.CASE_INSENSITIVE_ORDER; // Noncompliant [[sc=19;ec=48]]
    Object obj6 = CASE_INSENSITIVE_ORDER; // Compliant, already reported in the static import

    var.doSomethingElse(String.CASE_INSENSITIVE_ORDER); // Noncompliant [[sc=25;ec=54]]
    var.doSomethingElse(new String().CASE_INSENSITIVE_ORDER); // Noncompliant [[sc=25;ec=37]]
    var.doSomethingElse(String.valueOf(2)); // Noncompliant [[sc=25;ec=39]]
    System.out.println(var);
    var.doSomething(new Integer(1));
    return new Unknown();
  }
}
class B extends String { // Noncompliant [[sc=17;ec=23]]
  public B(Integer a) {
  }
  public void doSomething(Integer a) {
  }
  public void doSomethingElse(Object a) {
  }
  public Unknown doSomething() { // Compliant
    return new Unknown();
  }
}

class C {
  void foo(Object o) {
    ((java.util.Optional<String>) o).map(value -> 1); // Compliant, ignore inferred type
  }
}
