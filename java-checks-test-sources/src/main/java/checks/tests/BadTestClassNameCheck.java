package checks.tests;

import org.junit.Test;

class BadTestClassNameCheck { // Noncompliant {{Rename class "BadTestClassNameCheck" to match the regular expression: '^((Test|IT)[a-zA-Z0-9_]+|[A-Z][a-zA-Z0-9_]*(Test|Tests|TestCase|IT|ITCase))$'}}
  @Test
  void foo() {}
}

abstract class AbstractBadTestClassNameCheck { // compliant
  @Test
  void foo() {}
}

class BadTestClassName_Check { // Noncompliant
  @Test
  void foo() {}
}

class BadTestClassNameCheckTest { // Compliant
  @org.testng.annotations.Test
  void foo() {}
}

class BadTestClassNameCheckSomeTests { // Compliant
  @Test
  void foo() {}
}

class Bad_Test_Class_Name_Check_Some_Tests { // Compliant
  @Test
  void foo() {}
}

class BadTestClassNameCheck2 { // Compliant
  void foo() {}

  BadTestClassNameCheck a = new BadTestClassNameCheck() {};
}
class TestBadTestClassNameCheck {
  @Test
  void foo() {
  }
}
class BadTestClassNameCheckTestCase {
  @Test
  void foo() {
  }
}
class ITBadTestClassNameCheck {
  @Test
  void foo() {
  }
}
class BadTestClassNameCheckIT {
  @Test
  void foo() {
  }
}
class BadTestClassNameCheckITCase {
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
