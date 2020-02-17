package checks.naming;

class A {
  String A; // Noncompliant {{Rename field "A"}} [[sc=10;ec=11]]
  public String B;
  void A() {
  }
}
class B extends A {
}
class aClass {
  String AcLass; // Noncompliant [[sc=10;ec=16]]
  aClass AClass; // Noncompliant
  void method() {
    String AcLaSS;
  }
}
class AnotherClass {
  static AnotherClass anotherClass; //compliant for singletons
}
