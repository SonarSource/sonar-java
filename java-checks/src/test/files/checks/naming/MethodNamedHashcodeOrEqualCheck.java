class A {
  int hashcode() { // Noncompliant {{Either override Object.hashCode(), or totally rename the method to prevent any confusion.}}
    return 0;
  }

  void hashcode() { // Noncompliant
  }

  <T> void hashcode() { // Noncompliant
  }

  int hashcode(int a, int b) { // Noncompliant
    return a + b;
  }

  int hashCode() {
    return 0;
  }

  void equal() { // Noncompliant [[sc=8;ec=13]] {{Either override Object.equals(Object obj), or totally rename the method to prevent any confusion.}}
  }

  int equal(Object obj) { // Noncompliant
    return 0;
  }

  int equals(Object obj) {
    return 0;
  }

  void tostring() { // Noncompliant [[sc=8;ec=16]] {{Either override Object.toString(), or totally rename the method to prevent any confusion.}}
  }

  void toString() { // Compliant
  }
}

interface B {
  void hashcode(); // Noncompliant

  @Foo
  @Bar
  int hashcode(); // Noncompliant
  int foo();
  
  int equal(); // Noncompliant
}
