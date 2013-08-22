class A {
  int hashcode() { // Non-Compliant
    return 0;
  }

  void hashcode() { // Non-Compliant
  }

  <T> void hashcode() { // Non-Compliant
  }

  int hashcode(int a, int b) { // Non-Compliant
    return a + b;
  }

  int hashCode() { // Compliant
    return 0;
  }
}

interface A {
  void hashcode(); // Non-Compliant

  @Foo
  @Bar
  int hashcode(); // Non-Compliant
  int foo(); // Compliant
}
