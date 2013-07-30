class A {
  private void f() {
    boolean result1 = foo.toUpperCase().equals(bar);             // Non-Compliant
    boolean result2 = foo.equals(bar.toUpperCase());             // Non-Compliant
    boolean result3 = foo.toLowerCase().equals(bar.LowerCase()); // Non-Compliant
    boolean result = foo.equalsIgnoreCase(bar);                  // Compliant

    String foo = foo.toUpperCase();                              // Compliant
    int a = foo.toUpperCase().compareTo("foo");                  // Compliant
  }
}
