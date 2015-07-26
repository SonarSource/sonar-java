class A {
  private void f() {
    System.out.println("" + String.valueOf(0)); // Noncompliant {{Directly append the argument of String.valueOf().}}
    System.out.println("" + String.valueOf(null, 0, 0)); // Compliant
    System.out.println(String.valueOf(0)); // Compliant
    System.out.println("" + ""); // Compliant
    System.out.println(String.valueOf(0) + ""); // Compliant
    System.out.println("" + "foo" +
      String.valueOf('a') + // Noncompliant
      String.valueOf('b') + // Noncompliant
      "");
    System.out.println("" + String.valueOf()); // Compliant
    System.out.println("" + String.foo(0)); // Compliant
    System.out.println("" + foo.valueOf(0)); // Compliant
    System.out.println("" + String.valueOf); // Compliant
    System.out.println("" + String.valueOf[0]); // Compliant
    System.out.println("" + String.valueOf.bar(0)); // Compliant

    ("" + String.valueOf('a')) + ""; // Noncompliant
    "" + ("" + String.valueOf('a')); // Noncompliant

    0 + String.valueOf('a'); // Compliant
    buf = buf + "tab @" + String.valueOf(position); // Noncompliant
  }
}
