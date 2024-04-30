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
      public void f() { // Noncompliant {{This method has 5 returns, which is more than the 4 allowed.}}
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
    bar.sort((o1, o2)-> { // Noncompliant {{This method has 6 returns, which is more than the 4 allowed.}}
//                   ^^
      if(false) return o2;
//              ^^^^^^<
      if(false) return o1;
//              ^^^^^^<
      if(false) return o2;
//              ^^^^^^<
      if(false) return o1;
//              ^^^^^^<
      if(false) return o1;
//              ^^^^^^<
      return o1;
//    ^^^^^^<
    });
    return null;
  }

  interface I {
    default void method(boolean a) { // Noncompliant {{This method has 5 returns, which is more than the 4 allowed.}}
      if(a) return;
      if(a) return;
      if(a) return;
      if(a) return;
      if(a) return;
    }
  }
}
