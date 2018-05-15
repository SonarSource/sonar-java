import unknown;
import java.lang.Object;
import java.lang.String; // Noncompliant
import java.lang.String.*; // Noncompliant
import static java.lang.String.valueOf; // Noncompliant

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
    Object obj2 = valueOf(3); // Noncompliant [[sc=19;ec=26]]
    Object obj3 = unknown(3);
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
  public Unknown doSomething() { // Compliant
    return new Unknown();
  }
}

class C {
  void foo(Object o) {
    ((java.util.Optional<String>) o).map(value -> 1); // Compliant, ignore inferred type
  }
}
