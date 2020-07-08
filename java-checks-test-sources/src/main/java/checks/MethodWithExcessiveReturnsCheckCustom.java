package checks;

import java.util.List;

class MethodWithExcessiveReturnsCheckCustom {
  boolean foo1() { // Compliant
   if (false) return true;
   return false;
  }

  boolean foo2() {
    if (false) return true;
    if (false) return false;
    if (false) return true;
    return false;
  }

  void foo3() {
    if (false) return;
    if (false) return;

    new MethodWithExcessiveReturnsCheckCustom() {
      public void f() { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 4.}}
        if (false) return;
        if (false) return;
        if (false) return;
        if (false) return;
        return;
      }

      public void g() { // Compliant
        if (false) return;
        if (false) return;
        return;
      }
    };

    if (false) return;
    return;
  }

  Object foo4(List<Integer> bar) {
    bar.sort((o1, o2)-> {
      if(false) return o2;
      if(false) return o1;
      if(false) return o2;
      return o1;
    });
    bar.sort((o1, o2)-> { // Noncompliant [[sc=22;ec=24]] {{Reduce the number of returns of this method 6, down to the maximum allowed 4.}}
      if(false) return o2;
      if(false) return o1;
      if(false) return o2;
      if(false) return o1;
      if(false) return o1;
      return o1;
    });
    return null;
  }

  interface I {
    default void method(boolean a) { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 4.}}
      if(a) return;
      if(a) return;
      if(a) return;
      if(a) return;
      if(a) return;
    }
  }
}
