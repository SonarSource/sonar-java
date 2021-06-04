
class JUnit45MethodAnnotationCheck_JUnit4_Extends_Unknown extends Unknown {
  @org.junit.Test void test() { }

  @Override
  public void setUp() { } // Compliant
}

class JUnit45MethodAnnotationCheck_JUnit4_Extends_Unknown_FP extends Unknown {
  @org.junit.Test void test() { }

  // Due to incomplete semantic, no way to known if this is an override or not.
  public void setUp() { } // Compliant
}
