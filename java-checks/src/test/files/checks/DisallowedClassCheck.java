class A {
  private String disallowed; // Noncompliant [[sc=11;ec=17]]
  private Integer disallowed2;
  private java.util.Date date;
  String method1() { // Noncompliant [[sc=3;ec=9]]
  }
  Unknown method(String param) { // Noncompliant [[sc=18;ec=24]]
    B var = new B(); // Compliant, no subtypes checked
    String str = new String(); // Noncompliant [[sc=5;ec=11]]
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
    ((java.util.Optional<String>) o).map(value -> 1);
  }
}
