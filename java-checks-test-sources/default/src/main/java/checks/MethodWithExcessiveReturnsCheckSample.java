package checks;

import java.util.Objects;

class MethodWithExcessiveReturnsCheckSample {
  boolean foo1() { // Compliant
   if (false) return true;
   return false;
  }

  boolean foo2() { // Noncompliant {{This method has 4 returns, which is more than the 3 allowed.}}
//        ^^^^
    if (false) return true;
//  ^^^<
    if (false) return false;
//  ^^^<
    if (false) return true;
//  ^^^<
    return false;
//  ^^^<
  }

  void foo3() { // Noncompliant {{This method has 4 returns, which is more than the 3 allowed.}}
    if (false) return;
    if (false) return;

    new MethodWithExcessiveReturnsCheckSample() {
      public void f() { // Noncompliant {{This method has 5 returns, which is more than the 3 allowed.}}
//                ^
        if (false) return;
//  ^^^<
        if (false) return;
//  ^^^<
        if (false) return;
//  ^^^<
        if (false) return;
//  ^^^<
        return;
//  ^^^<
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

  int intMember;
  String stringMember;

  @Override
  public boolean equals(Object obj) { // Compliant because equals method is excempt from the rule
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MethodWithExcessiveReturnsCheckSample other = (MethodWithExcessiveReturnsCheckSample) obj;
    return intMember == other.intMember && Objects.equals(stringMember, other.stringMember);
  }

  public boolean equals(MethodWithExcessiveReturnsCheckSample obj) { // Noncompliant
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return intMember == obj.intMember && Objects.equals(stringMember, obj.stringMember);
  }

  interface I {
    default void method() { // Noncompliant {{This method has 5 returns, which is more than the 3 allowed.}}
      if (false) return;
      if (false) return;
      if (false) return;
      if (false) return;
      return;
    }
  }
}
