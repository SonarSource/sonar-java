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

  int foo4(String path) { // Noncompliant {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    switch (path) {
      case "FOO":
        return 1;
      case "BAR":
        return 2;
      case "FIZ":
        return 3;
      default:
        return 4;
    }
  }

  int foo5(String path) { // Noncompliant {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    switch (path) {
      case "FOO":
        return 1;
      default:
        switch (path) {
          case "BAR":
            return 2;
          default:
            return 3;
        }
    }
    return 4;
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
