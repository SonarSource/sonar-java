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

  boolean foo1() { // Compliant
   if (false) return true;
   return false;
  }

  boolean foo2() { // Noncompliant [[sc=11;ec=15]] {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    return true;
    return false;
    return true;
    return false;
  }

  void foo3() { // Noncompliant {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    return;
    return;

    new A() {
      public void f() { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 3.}}
        return;
        return;
        return;
        return;
        return;
      }

      public void g() { // Compliant
        return;
        return;
        return;
      }
    };

    return;
    return;
  }
}
interface B {
  default void method() { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 3.}}
    return;
    return;
    return;
    return;
    return;
  }
}
