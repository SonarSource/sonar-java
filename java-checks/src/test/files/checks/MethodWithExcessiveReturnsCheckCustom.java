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

  boolean foo2() {
    return true;
    return false;
    return true;
    return false;
  }

  void foo3() {
    return;
    return;

    new A() {
      public void f() { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 4.}}
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

  Object foo4(List<Foo> bar) {
    bar.sort((o1, o2)-> {
      return o2;
      return o1;
      return o2;
      return o1;
    });
    bar.sort((o1, o2)-> { // Noncompliant {{Reduce the number of returns of this method 6, down to the maximum allowed 4.}}
      return o2;
      return o1;
      return o2;
      return o1;
      return o1;
      return o1;
    });
    return null;
  }
}
interface B {
  default void method() { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 4.}}
    return;
    return;
    return;
    return;
    return;
  }
}
