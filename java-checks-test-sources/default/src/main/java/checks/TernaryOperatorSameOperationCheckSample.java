package checks;

import java.util.function.Function;

class TernaryOperatorSameOperationCheckSample {

  boolean condition;
  boolean other;
  TernaryOperatorSameOperationCheckSample obj;
  TernaryOperatorSameOperationCheckSample obj2;

  void testMethodInvocations() {
    String a = "a";
    String b = "b";
    String x = "x";
    String y = "y";
    String z = "z";

    // Method invocations - Noncompliant
    String m1 = condition ? foo(a) : foo(b); // Noncompliant {{Move the conditional expression inside this operation.}}

    String m2 = condition ? this.foo(a) : this.foo(b); // Noncompliant
    String m3 = condition ? obj.foo(a) : obj.foo(b); // Noncompliant
    String m4 = condition ? StaticClass.foo(a) : StaticClass.foo(b); // Noncompliant

    // Method invocations with multiple args where some differ - Noncompliant
    String m5 = condition ? foo(a, x) : foo(b, x); // Noncompliant

    // Method invocations with multiple args where all differ - Compliant
    String m6 = condition ? foo(a, x) : foo(b, y); // Compliant - more than one argument differs

    // Method invocations - Compliant (different operations or same arguments)
    String c1 = condition ? foo(a) : bar(b); // Compliant - different methods
    String c2 = condition ? foo(a) : foo(a); // Compliant - same arguments
    String c3 = condition ? foo(a) : foo(a, b); // Compliant - different number of arguments
  }

  void testMethodInvocationsEdgeCases() {
    String a = "a";
    String b = "b";

    // No-arg methods - Compliant (no arguments to differ)
    String e1 = condition ? noArg() : noArg(); // Compliant

    // Different receivers - Compliant
    String e2 = condition ? obj.foo(a) : obj2.foo(b); // Compliant - different receiver objects

    // Different kinds in true/false - Compliant
    Object e3 = condition ? foo(a) : new Foo(b); // Compliant - method vs constructor
    Object e4 = condition ? a : b; // Compliant - simple identifiers, not method/new/array

    // String literal - Compliant
    String e5 = condition ? "hello" : "world"; // Compliant

    // Numeric literal - Compliant
    int e6 = condition ? 1 : 2; // Compliant
  }

  void testNewClass() {
    String a = "a";
    String b = "b";

    // New class - Noncompliant
    Object n1 = condition ? new Foo(a) : new Foo(b); // Noncompliant {{Move the conditional expression inside this operation.}}

    // New class with multiple args where some differ - Noncompliant
    Object n2 = condition ? new Foo(a, b) : new Foo(b, b); // Noncompliant

    // New class with multiple args where all differ - Compliant
    Object n3 = condition ? new Foo(a, a) : new Foo(b, b); // Compliant - more than one argument differs

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

  void testNestedTernary() {
    String x = "x";
    String y = "y";
    String z = "z";

    // Nested ternary - Noncompliant (outer ternary has same operation after parentheses skip)
    String n3 = condition ? (other ? foo(x) : foo(y)) : foo(z); // Noncompliant {{Move the conditional expression inside this operation.}}

    // Nested ternary - Compliant (inner ternary is not same operation)
    String c9 = condition ? (other ? foo(x) : bar(x)) : foo(z); // Compliant
  }

  void testMemberSelectEdgeCases() {
    String a = "a";
    String b = "b";

    // Same receiver, different method names - Compliant
    String ms1 = condition ? obj.foo(a) : obj.bar(b); // Compliant - different method names

    // Method invocation vs member select method invocation - Compliant
    String ms2 = condition ? foo(a) : obj.foo(b); // Compliant - identifier vs member select
  }

  void testOther() {
    String a = "a";
    String b = "b";

    // Method reference - Compliant
    Function<String, String> f1 = condition ? this::foo : this::method; // Compliant

    // Multiple different operations - Compliant
    String c10 = condition ? foo(a) : bar(b); // Compliant
    Object c11 = condition ? new Foo(a) : new Bar(b); // Compliant

    // Array access vs method invocation - Compliant
    String[] arr = {a, b};
    Object o1 = condition ? arr[0] : foo(b); // Compliant - different expression kinds

    // New class vs array access - Compliant
    Object o2 = condition ? new Foo(a) : arr[0]; // Compliant - different expression kinds
  }

  // Private methods used in ternary
  private String foo(String s) { return s; }
  private String foo(String s, String t) { return s + t; }
  private String bar(String s) { return s; }
  private String method(String s) { return s; }
  private String noArg() { return ""; }

  private static class StaticClass {
    static String foo(String s) { return s; }
  }

  private static class Foo {
    Foo(String s) {}
    Foo(String s, String t) {}
  }

  private static class Bar {
    Bar(String s) {}
  }

}
