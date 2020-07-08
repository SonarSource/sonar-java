class A {
  {
    return; // Compliant
    return;
  }

  {
    return; // Compliant
    return;
    return;
    return;
  }

  void foo() { // Noncompliant [[sc=8;ec=11]] {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    return;
    return;
    return;
    return;
  }

  boolean foo2() { // Noncompliant [[sc=11;ec=15]] {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    return true;
    return false;
    return true;
    return false;
  }
}
