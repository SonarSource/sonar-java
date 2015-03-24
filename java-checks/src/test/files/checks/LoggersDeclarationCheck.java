class A {

  private static final Logger LOG; // Compliant

  static final Logger LOG; // Noncompliant
  private final Logger LOG; // Noncompliant
  private static Logger LOG; // Noncompliant

  private static final Logger foo; // Noncompliant

  private static final Log LOG; // Compliant
  private static final Log LOGGER; // Compliant

  private static final Log foo; // Noncompliant

  private static final Foo foo; // Compliant

  private static final Log[] foo; // Compliant

  private static Log foo; // Noncompliant

  public A() {
    final Logger LOG; // Noncompliant
    int a = 0; // Compliant
  }
  
  private void logExclusions(String[] exclusions, Logger logger) { // Compliant
  }
}
