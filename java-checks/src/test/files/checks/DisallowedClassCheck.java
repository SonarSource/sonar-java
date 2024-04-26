import unknown;
import java.lang.Object;
import java.lang.String; // Noncompliant
import java.lang.String.*; // Noncompliant
import static java.lang.String.valueOf; // Noncompliant
import static java.lang.String.CASE_INSENSITIVE_ORDER; // Noncompliant

class A {
  private String disallowed; // Noncompliant
//        ^^^^^^
  private Integer allowed;
  private java.util.Date date;
  String method1() { // Noncompliant
//^^^^^^
  }
  Unknown method(String param) { // Noncompliant
//               ^^^^^^
    B var = new B(); // Compliant, no subtypes checked
    String str = new String(); // Noncompliant
//  ^^^^^^
    Object obj1 = String.valueOf(3); // Noncompliant
//                ^^^^^^^^^^^^^^
    Object obj2 = valueOf(3); // Compliant, already reported in the static import
    Object obj3 = unknown(3);
    Object obj4 = java.lang.String.CASE_INSENSITIVE_ORDER; // Noncompliant
//                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Object obj5 = String.CASE_INSENSITIVE_ORDER; // Noncompliant
//                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    Object obj6 = CASE_INSENSITIVE_ORDER; // Compliant, already reported in the static import

    var.doSomethingElse(String.CASE_INSENSITIVE_ORDER); // Noncompliant
//                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    var.doSomethingElse(new String().CASE_INSENSITIVE_ORDER); // Noncompliant
//                      ^^^^^^^^^^^^
    var.doSomethingElse(String.valueOf(2)); // Noncompliant
//                      ^^^^^^^^^^^^^^
    System.out.println(var);
    var.doSomething(new Integer(1));
    return new Unknown();
  }

  void usedWithReflection() throws ClassNotFoundException {
    Class c = Class.forName("java.lang.String"); // Noncompliant
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
}
class B extends String { // Noncompliant
//              ^^^^^^
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
