class A {

  private static final Logger LOG; // Compliant

  static final Logger LOG2; // Noncompliant
  private final Logger LOG3; // Noncompliant
  private static Logger LOG4; // Noncompliant

  private static final Logger foo0;

  private static final Log LOG5; // Compliant
  private static final Log LOGGER; // Compliant

  private static final Log foo;

  private static final Foo foo1; // Compliant

  private static final Log[] foo2; // Compliant

  private static Log foo3; // Noncompliant {{Make the "foo3" logger private static final.}}

  public A() {
    final Logger LOG; // Noncompliant {{Make the "LOG" logger private static final.}}
    int a = 0; // Compliant
  }
  
  private void logExclusions(String[] exclusions, Logger logger) { // Compliant
  }
}
