class A {
  private void f() {
    boolean result = foo.toLowerCase().equals(bar);             // Noncompliant [[sc=22;ec=51]] {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    result = foo.toUpperCase().equals(bar);             // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    result = "foo".toUpperCase().equals(bar);           // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    result = foo.equals(bar.toLowerCase());             // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    result = foo.equals(bar.toUpperCase());             // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    result = "foo".equals(bar.toUpperCase());           // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}

    foo.toLowerCase().equals(bar.toLowerCase());                // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    foo.equals(toLowerCase());                                  // Compliant
    foo.equals(something().somethingElse().toUpperCase());      // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    foo.equals("bar".toLowerCase());                            // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}

    result = foo.equalsIgnoreCase(bar);                 // Compliant

    String foo = foo.toUpperCase();                             // Compliant
    int a = foo.toUpperCase().compareTo("foo");                 // Compliant

    StringUtils.equals("foo", "bar".toLowerCase());             // Compliant
    foo.equals();                                               // Compliant
    foo.toLowerCase().equals[0];                                // Compliant

    foo.toLowerCase().toLowerCase().equals(bar);                // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
    foo.toLowerCase().toUpperCase().equals(bar);                // Noncompliant {{Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.}}
  }
}
