package checks;

import java.util.function.Function;

class TernaryOperatorSameOperationCheckNoSemanticSample {

  boolean condition;
  boolean other;
  TernaryOperatorSameOperationCheckNoSemanticSample obj;
  TernaryOperatorSameOperationCheckNoSemanticSample obj2;

  void testMethodInvocations() {
    String a = "a";
    String b = "b";
    String x = "x";
    String y = "y";

    // Method invocations - Noncompliant
    String m1 = condition ? foo(a) : foo(b); // Noncompliant {{Move the conditional expression inside this operation.}}

    String m2 = condition ? this.foo(a) : this.foo(b); // Noncompliant
    String m3 = condition ? obj.foo(a) : obj.foo(b); // Noncompliant

    // Method invocations with multiple args where some differ - Noncompliant
    String m5 = condition ? foo(a, x) : foo(b, x); // Noncompliant

    // Method invocations with multiple args where all differ - Compliant
    String m6 = condition ? foo(a, x) : foo(b, y); // Compliant - more than one argument differs

    // Method invocations - Compliant (different operations or same arguments)
    String c1 = condition ? foo(a) : bar(b); // Compliant - different methods
    String c2 = condition ? foo(a) : foo(a); // Compliant - same arguments
    String c3 = condition ? foo(a) : foo(a, b); // Compliant - different number of arguments
  }

  void testNewClass() {
    String a = "a";
    String b = "b";

    // New class - Noncompliant
    Object n1 = condition ? new Foo(a) : new Foo(b); // Noncompliant {{Move the conditional expression inside this operation.}}

    // New class - Compliant
    Object c4 = condition ? new Foo(a) : new Bar(b); // Compliant - different classes
    Object c5 = condition ? new Foo(a) : new Foo(a); // Compliant - same arguments
    Object c6 = condition ? new Foo(a) : new Foo(a, b); // Compliant - different arguments count
  }

  void testArrayAccess() {
    String[] arr = new String[10];
    String[] otherArr = new String[10];
    int i = 0;
    int j = 1;

    // Array access - Noncompliant
    String a1 = condition ? arr[i] : arr[j]; // Noncompliant {{Move the conditional expression inside this operation.}}

    // Array access - Compliant
    String c7 = condition ? arr[i] : arr[i]; // Compliant - same index
    String c8 = condition ? arr[i] : otherArr[j]; // Compliant - different arrays
  }

  void testNewClassWithClassBody() {
    String a = "a";
    String b = "b";

    // Anonymous class body - Compliant even without semantics
    Object ac1 = condition ? new Foo(a) { } : new Foo(b) { }; // Compliant - anonymous class bodies
    Object ac2 = condition ? new Foo(a) { } : new Foo(b); // Compliant - one has class body
  }

  // Private methods used in ternary
  private String foo(String s) { return s; }
  private String foo(String s, String t) { return s + t; }
  private String bar(String s) { return s; }
  private String noArg() { return ""; }

  private static class Foo {
    Foo(String s) {}
    Foo(String s, String t) {}
  }

  private static class Bar {
    Bar(String s) {}
  }

}
