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

  boolean foo2() { // Non-Compliant
    return true;
    return false;
    return true;
    return false;
  }

  void foo3() { // Non-Compliant
    return;
    return;

    new A() {
      public void f() { // Non-Compliant
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
