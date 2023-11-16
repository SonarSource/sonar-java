package checks.tests;

public class NoTestInTestClassCustomPattern {
}

class TestJUnit4WithJUnit3 { // Noncompliant [[sc=7;ec=27]]
  public void test() {
  }
}

class JUnit4WithJUnit3Test { // Noncompliant [[sc=7;ec=27]]
  public void test() {
  }
}

class JUnit4WithJUnit3Tests { // Noncompliant [[sc=7;ec=28]]
  public void test() {
  }
}

class JUnit4WithJUnit3TestCase { // Noncompliant [[sc=7;ec=31]]
  public void test() {
  }
}
