package checks.tests;

class JUnitMethodDeclarationCheck_JUnit4 {
  @org.junit.Test void test() { }

  public void setUp() { } // Noncompliant {{Annotate this method with JUnit4 '@org.junit.Before' or remove it.}}
  public void tearDown() { }  // Noncompliant {{Annotate this method with JUnit4 '@org.junit.After' or remove it.}}

  public static junit.framework.Test suite() { return null; }  // Noncompliant {{Remove this method, JUnit4 test suites are not relying on it anymore.}}
}

class UnitMethodDeclarationCheck_JUnit4_other_suite {
  @org.junit.Test void test() {}
  Integer suite() { return null; } // Compliant
}

class JUnitMethodDeclarationCheck_JUnit4_compliant {
  protected Object step() { return null; } // unrelated
  protected Object teaDown() { return null; } // typo from tearDown, but could be unrelated
  @org.junit.Test void test() { }
  @org.junit.Before public void setUp() { }
  @org.junit.After public void tearDown() { }
}

class JUnitMethodDeclarationCheck_JUnit5 {
  @org.junit.jupiter.api.Test void test() { }

  public void setUp() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.BeforeEach' or remove it.}}
  public void tearDown() { }  // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.AfterEach' or remove it.}}

  public static junit.framework.Test suite() { return null; }  // Noncompliant {{Remove this method, JUnit5 test suites are not relying on it anymore.}}
}

class JUnitMethodDeclarationCheck_JUnit5_compliant {
  @org.junit.jupiter.api.Test void test() { }

  @org.junit.jupiter.api.BeforeEach public void setUp() { }
  @org.junit.jupiter.api.AfterEach public void tearDown() { }
}
