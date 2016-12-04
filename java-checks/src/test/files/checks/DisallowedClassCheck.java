class A {
  private String disallowed; // Noncompliant [[sc=11;ec=17]]
  private Integer disallowed2;
  private java.util.Date date;
  String method1() { // Noncompliant [[sc=3;ec=9]]
  }
void method(String param) { // Noncompliant [[sc=13;ec=19]]
  B var = new B(); // Compliant, no subtypes checked
  String str = new String(); // Noncompliant [[sc=3;ec=9]]
  System.out.println(var);
  var.doSomething(new Integer(1));
}
}
class B extends String { // Compliant, can extend
  public B(Integer a) {
  }
  public void doSomething(Integer a) {
  }
}
