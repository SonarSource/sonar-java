package checks.tests;

import org.junit.Test;

class BadTestClassNameCheckSample { // Noncompliant {{Rename class "BadTestClassNameCheckSample" to match the regular expression: '^((Test|IT)[a-zA-Z0-9_]+|[A-Z][a-zA-Z0-9_]*(Test|Tests|TestCase|IT|ITCase))$'}}
  @Test
  void foo() {}
}

abstract class AbstractBadTestClassNameCheckSample { // compliant
  @Test
  void foo() {}
}

class BadTestClassName_Check { // Noncompliant
  @Test
  void foo() {}
}

class BadTestClassNameCheckSampleTest { // Compliant
  @org.testng.annotations.Test
  void foo() {}
}

class BadTestClassNameCheckSampleSomeTests { // Compliant
  @Test
  void foo() {}
}

class Bad_Test_Class_Name_Check_Some_Tests { // Compliant
  @Test
  void foo() {}
}

class BadTestClassNameCheckSample2 { // Compliant
  void foo() {}

  BadTestClassNameCheckSample a = new BadTestClassNameCheckSample() {};
}
class TestBadTestClassNameCheckSample {
  @Test
  void foo() {
  }
}
class BadTestClassNameCheckSampleTestCase {
  @Test
  void foo() {
  }
}
class ITBadTestClassNameCheckSample {
  @Test
  void foo() {
  }
}
class BadTestClassNameCheckSampleIT {
  @Test
  void foo() {
  }
}
class BadTestClassNameCheckSampleITCase {
  @Test
  void foo() {
  }
}

class JUnit5Noncompliant1 { // Noncompliant
  @org.junit.jupiter.api.Test
  void foo() {}
}

class JUnit5Noncompliant2 { // Noncompliant
  @org.junit.jupiter.api.RepeatedTest(2)
  void foo() {}
}

class JunitNestedTest {
  @org.junit.jupiter.api.Nested
  class Positive {
    @org.junit.jupiter.api.Test
    void foo() {}
  }
}

class JunitNested { // Noncompliant
  @org.junit.jupiter.api.Nested
  class Negative {
    @org.junit.jupiter.api.Test
    void foo() {}
  }
}
