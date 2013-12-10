class Foo {
  void foo() {
    int a = 0;                   // Compliant
    a = 0;                       // Compliant
    System.out.println(a);       // Compliant
    System.out.println(a = 0);   // Non-Compliant
    System.out.println(a += 0);  // Non-Compliant
    System.out.println(a == 0);  // Compliant

    a = b = 0;                   // Compliant
    a += foo[i];                 // Compliant

    _stack[
           index = 0             // Noncompliant
           ] = node;
  }
}
