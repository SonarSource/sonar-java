import org.junit.Test;

class A { // Noncompliant {{Rename class "A" to match the regular expression: '^[A-Z][a-zA-Z0-9]*SonarTest$'}}
  @Test
  void foo() {}
}

class ASonarTest { // Compliant
  @org.testng.annotations.Test
  void foo() {}
}

class B { // Compliant
  void foo() {}

  A a = new A() {};
}
