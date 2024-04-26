package checks.tests;

public class NoTestInTestClassCustomPattern {
}

class TestJUnit4WithJUnit3 { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^
  public void test() {
  }
}

class JUnit4WithJUnit3Test { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^
  public void test() {
  }
}

class JUnit4WithJUnit3Tests { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^
  public void test() {
  }
}

class JUnit4WithJUnit3TestCase { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^
  public void test() {
  }
}
