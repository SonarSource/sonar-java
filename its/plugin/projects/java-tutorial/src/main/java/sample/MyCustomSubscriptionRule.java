package sample;

class MyCustomSubscriptionRule {

  int foo() { return 0; }
  int foo(int a) { return 0; }  // Noncompliant {{message}}
  int foo(int a, int b) { return 0; }

  Object foo(Object a){ return null; } // Noncompliant {{message}}
  String bar(String a){ return null; } // Noncompliant {{message}}
  String qix(Object a){ return null; }
}