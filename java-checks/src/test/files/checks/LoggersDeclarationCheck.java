class A {

  private org.apache.maven.plugin.logging.Log foo = getLog();

  private static final Logger LOG; // Compliant

  static final Logger LOG; // Noncompliant
  private final Logger LOG; // Noncompliant
  private static Logger LOG; // Noncompliant

  private static final Logger foo; // Noncompliant

  private static final Log LOG; // Compliant
  private static final Log LOGGER; // Compliant

  private static final Log foo; // Noncompliant {{Rename the "foo" logger to comply with the format "LOG(?:GER)?".}}

  private static final Foo foo; // Compliant

  private static final Log[] foo; // Compliant

  private static Log foo; // Noncompliant {{Make the "foo" logger private static final and rename it to comply with the format "LOG(?:GER)?".}}

  public A() {
    final Logger LOG; // Noncompliant {{Make the "LOG" logger private static final.}}
    int a = 0; // Compliant
  }
  
  private void logExclusions(String[] exclusions, Logger logger) { // Compliant
  }
}
