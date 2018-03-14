class A {

  private org.apache.maven.plugin.logging.Log foo0 = getLog();

  private static final Logger LOG; // Compliant

  static final Logger LOG2; // Noncompliant
  private final Logger LOG3; // Noncompliant
  private static Logger LOG4; // Noncompliant

  private static final Logger foo1; // Noncompliant

  private static final Log LOGGER; // Compliant

  private static final Log foo; // Noncompliant [[sc=28;ec=31]] {{Rename the "foo" logger to comply with the format "LOG(?:GER)?".}}

  private static final Foo foo2; // Compliant

  private static final Log[] foo3; // Compliant

  private static Log foo4; // Noncompliant {{Make the "foo4" logger private static final and rename it to comply with the format "LOG(?:GER)?".}}

  public A() {
    final Logger LOG; // Noncompliant {{Make the "LOG" logger private static final.}}
    int a = 0; // Compliant
  }
  
  private void logExclusions(String[] exclusions, Logger logger) { // Compliant
  }

  protected final org.slf4j.Logger logger; // Noncompliant
 
}
