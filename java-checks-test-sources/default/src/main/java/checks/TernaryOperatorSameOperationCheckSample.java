package checks;

class TernaryOperatorSameOperationCheckSample {

  boolean condition;
  String result;
  String[] array;

  // Method invocations - Noncompliant
  String m1 = condition ? foo(a) : foo(b); // Noncompliant {{Move the conditional expression inside this operation.}}

  String m2 = condition ? this.foo(a) : this.foo(b); // Noncompliant
  String m3 = condition ? obj.foo(a) : obj.foo(b); // Noncompliant
  String m4 = condition ? StaticClass.foo(a) : StaticClass.foo(b); // Noncompliant

  // Method invocations - Compliant (different operations or same arguments)
  String c1 = condition ? foo(a) : bar(b); // Compliant
  String c2 = condition ? foo(a) : foo(a); // Compliant (same arguments)
  String c3 = condition ? foo(a) : foo(a, b); // Compliant (different number of arguments)

  // New class - Noncompliant
  Object n1 = condition ? new Foo(a) : new Foo(b); // Noncompliant {{Move the conditional expression inside this operation.}}

  // New class - Compliant
  Object c4 = condition ? new Foo(a) : new Bar(b); // Compliant (different classes)
  Object c5 = condition ? new Foo(a) : new Foo(a); // Compliant (same arguments)
  Object c6 = condition ? new Foo(a) : new Foo(a, b); // Compliant (different arguments)

  // Array access - Noncompliant
  String[] arr = new String[10];
  String a1 = condition ? arr[a] : arr[b]; // Noncompliant {{Move the conditional expression inside this operation.}}

  // Array access - Compliant
  String c7 = condition ? arr[a] : arr[a]; // Compliant (same index)
  String c8 = condition ? arr[a] : otherArr[b]; // Compliant (different arrays)

  // Nested ternary - Noncompliant (outer ternary)
  String n3 = condition ? (other ? foo(x) : foo(y)) : foo(z); // Noncompliant {{Move the conditional expression inside this operation.}}

  // Nested ternary - Compliant (inner ternary is not same operation)
  String c9 = condition ? (other ? foo(x) : bar(x)) : foo(z); // Compliant

  // Method reference - Compliant (different method references)
  java.util.function.Function<String, String> f1 = condition ? this::foo : this::method; // Compliant

  // Multiple different operations - Compliant
  String c10 = condition ? foo(a) : bar(b); // Compliant
  String c11 = condition ? new Foo(a) : new Bar(b); // Compliant

  // Private methods used in ternary
  private String foo(String s) { return s; }
  private String foo(String s, String t) { return s + t; }
  private void method(String a, String b) {}

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
