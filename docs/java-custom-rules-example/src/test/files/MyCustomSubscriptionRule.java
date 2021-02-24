class A {

  int foo() {}
  int foo(int a) {} // Noncompliant {{message}}
  int foo(int a, int b) {}

  Object foo(Object a){} // Noncompliant {{message}}
  String bar(String a){} // Noncompliant {{message}}
  String qix(Object a){}
}