package checks.naming;

class A {
  String A; // Noncompliant {{Rename field "A"}}
//       ^
  public String B;
  void A() {
  }
}
class B extends A {
}
class aClass {
  String AcLass; // Noncompliant
//       ^^^^^^
  aClass AClass; // Noncompliant
  void method() {
    String AcLaSS;
  }
}
class AnotherClass {
  static AnotherClass anotherClass; //compliant for singletons
}
