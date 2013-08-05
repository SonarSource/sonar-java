class A {
  private void f() {
    boolean result = foo.toLowerCase().equals(bar);             // Non-Compliant
    boolean result = foo.toUpperCase().equals(bar);             // Non-Compliant
    boolean result = "foo".toUpperCase().equals(bar);           // Non-Compliant
    boolean result = foo.equals(bar.toLowerCase());             // Non-Compliant
    boolean result = foo.equals(bar.toUpperCase());             // Non-Compliant
    boolean result = "foo".equals(bar.toUpperCase());           // Non-Compliant

    foo.toLowerCase().equals(bar.toLowerCase());                // Non-Compliant
    foo.equals(toLowerCase());                                  // Compliant
    foo.equals(something().somethingElse().toUpperCase());      // Non-Compliant
    foo.equals("bar".toLowerCase());                            // Non-Compliant

    boolean result = foo.equalsIgnoreCase(bar);                 // Compliant

    String foo = foo.toUpperCase();                             // Compliant
    int a = foo.toUpperCase().compareTo("foo");                 // Compliant

    StringUtils.equals("foo", "bar".toLowerCase());             // Compliant
    foo.equals();                                               // Compliant
    foo.toLowerCase().equals[0];                                // Compliant

    foo.toLowerCase().toLowerCase().equals(bar);                // Non-Compliant
    foo.toLowerCase().toUpperCase().equals(bar);                // Non-Compliant
  }
}
