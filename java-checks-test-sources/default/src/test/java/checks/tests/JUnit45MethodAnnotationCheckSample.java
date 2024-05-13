package checks.tests;

class JUnit45MethodAnnotationCheckSample_JUnit4 {
  @org.junit.Test void test() { }

  public void setUp() { } // Noncompliant {{Annotate this method with JUnit4 '@org.junit.Before' or rename it to avoid confusion.}}
//            ^^^^^
  public void tearDown() { } // Noncompliant {{Annotate this method with JUnit4 '@org.junit.After' or rename it to avoid confusion.}}
}

class JUnit45MethodAnnotationCheckSample_JUnit4_compliant {
  protected Object step() { return null; } // unrelated
  protected Object teaDown() { return null; } // typo from tearDown, but could be unrelated
  public void setUp(boolean b) { } // Compliant, setUp with argument are excluded
  public void tearDown(String s) { } // Compliant, tearDown with argument are excluded
  @org.junit.Test void test() { }
  @org.junit.Before public void setUp() { }
  @org.junit.After public void tearDown() { }
}

class JUnit45MethodAnnotationCheckSample_JUnit4_compliant2 {
  @org.junit.Test void test() { }
  @org.junit.BeforeClass public static void setUp() { }
  @org.junit.AfterClass public static void tearDown() { }
}

class JUnit45MethodAnnotationCheckSample_JUnit4_compliant_private_setup {
  @org.junit.Test void test() { }
  private void setUp() { }
  private void tearDown() { }
}

abstract class AbstractJUnit45MethodAnnotationCheckSample_JUnit4 {
  @org.junit.Before public void setUp() { }
}

class JUnit45MethodAnnotationCheckSample_JUnit4_compliant3 extends AbstractJUnit45MethodAnnotationCheckSample_JUnit4 {
  @org.junit.Test void test() { }

  @Override
  public void setUp() { } // Compliant
}

class JUnit45MethodAnnotationCheckSample_JUnit5 {
  @org.junit.jupiter.api.Test void test() { }

  public void setUp() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.BeforeEach' or rename it to avoid confusion.}}
  public void tearDown() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.AfterEach' or rename it to avoid confusion.}}
}

class JUnit45MethodAnnotationCheckSample_JUnit5_compliant {
  @org.junit.jupiter.api.Test void test() { }
  @org.junit.jupiter.api.BeforeEach public void setUp() { }
  @org.junit.jupiter.api.AfterEach public void tearDown() { }
}

class JUnit45MethodAnnotationCheckSample_JUnit5_compliant2 {
  @org.junit.jupiter.api.Test void test() { }
  @org.junit.jupiter.api.BeforeAll public static void setUp() { }
  @org.junit.jupiter.api.AfterAll public static void tearDown() { }
}

class JUnit45MethodAnnotationCheckSample_JUnit4_5_mixed {
  @org.junit.Test void junit4() { }
  @org.junit.jupiter.api.Test void junit5() { }

  // use JUnit 4 annotations
  @org.junit.Before public void setUp() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.BeforeEach' instead of JUnit4 '@Before'.}}
  @org.junit.After public void tearDown() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.AfterEach' instead of JUnit4 '@After'.}}

  @org.junit.Before public void before() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.BeforeEach' instead of JUnit4 '@Before'.}}
  @org.junit.After public void after() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.AfterEach' instead of JUnit4 '@After'.}}
}

class JUnit45MethodAnnotationCheckSample_JUnit4_5_mixed2 {
  @org.junit.Test void junit4() { }
  @org.junit.jupiter.api.Test void junit5() { }

  // use JUnit 4 annotations
  @org.junit.BeforeClass public static void setUp() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.BeforeAll' instead of JUnit4 '@BeforeClass'.}}
  @org.junit.AfterClass public static void tearDown() { } // Noncompliant {{Annotate this method with JUnit5 '@org.junit.jupiter.api.AfterAll' instead of JUnit4 '@AfterClass'.}}
}

class JUnit45MethodAnnotationCheckSample_JUnit3 extends junit.framework.TestCase {
  // Compliant, no need for annotation in JUnit 3
  public void setUp() {  }
  public void tearDown() {  }
}

class JUnit45MethodAnnotationCheckSample_mixed_JUnit34 extends junit.framework.TestCase {
  public void setUp() {  } // Compliant
  public void tearDown() {  } // Compliant

  @org.junit.Test void junit4() { }
}

class JUnit4_5_mixed_but_compliant {
  @org.junit.Test void junit4() { }
  @org.junit.jupiter.api.Test void junit5() { }

  @org.junit.BeforeClass
  @org.junit.jupiter.api.BeforeAll
  public static void setUp() { } // Compliant

  @org.junit.AfterClass
  @org.junit.jupiter.api.AfterAll
  public static void tearDown() { } // compliant
}
