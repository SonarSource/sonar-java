class A {
  void foo(){}
}
interface I {
  void bar();
}
class B extends A implements I {
  void foo() {} //NonCompliant
  void bar() {} //NonCompliant
}


