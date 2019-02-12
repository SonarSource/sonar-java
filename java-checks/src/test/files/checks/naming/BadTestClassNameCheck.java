import org.junit.Test;

class A { // Noncompliant {{Rename class "A" to match the regular expression: '^((Test|IT)[a-zA-Z0-9]+|[A-Z][a-zA-Z0-9]*(Test|IT|TestCase|ITCase))$'}}
  @Test
  void foo() {}
}

class ATest { // Compliant
  @org.testng.annotations.Test
  void foo() {}
}

class B { // Compliant
  void foo() {}

  A a = new A() {};
}
class TestA {
  @Test
  void foo() {
  }
}
class ATestCase {
  @Test
  void foo() {
  }
}
class ITanIntegration {
  @Test
  void foo() {
  }
}
class AnIT {
  @Test
  void foo() {
  }
}
class AnITCase {
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
