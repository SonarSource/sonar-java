import org.junit.Test;

class A { // Noncompliant {{Rename class "A" to match the regular expression: '^[A-Z][a-zA-Z0-9]*Test$'}}
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
