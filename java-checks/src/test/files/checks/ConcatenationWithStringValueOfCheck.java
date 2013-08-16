class A {
  private void f() {
    System.out.println("" + String.valueOf(0)); // Non-Compliant
    System.out.println("" + String.valueOf(null, 0, 0)); // Compliant
    System.out.println(String.valueOf(0)); // Compliant
    System.out.println("" + ""); // Compliant
    System.out.println(String.valueOf(0) + ""); // Compliant
    System.out.println("" + "foo" +
      String.valueOf('a') + // Non-Compliant
      "");
    System.out.println("" + String.valueOf()); // Compliant
    System.out.println("" + String.foo(0)); // Compliant
    System.out.println("" + foo.valueOf(0)); // Compliant
    System.out.println("" + String.valueOf); // Compliant
    System.out.println("" + String.valueOf[0]); // Compliant
    System.out.println("" + String.valueOf.bar(0)); // Compliant
  }
}
