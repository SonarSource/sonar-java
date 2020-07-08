package checks;

import java.util.Objects;

class MethodWithExcessiveReturnsCheck {
  boolean foo1() { // Compliant
   if (false) return true;
   return false;
  }

  boolean foo2() { // Noncompliant [[sc=11;ec=15]] {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    if (false) return true;
    if (false) return false;
    if (false) return true;
    return false;
  }

  void foo3() { // Noncompliant {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    if (false) return;
    if (false) return;

    new MethodWithExcessiveReturnsCheck() {
      public void f() { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 3.}}
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
    MethodWithExcessiveReturnsCheck other = (MethodWithExcessiveReturnsCheck) obj;
    return intMember == other.intMember && Objects.equals(stringMember, other.stringMember);
  }

  public boolean equals(MethodWithExcessiveReturnsCheck obj) { // Noncompliant because this is not a proper equals method
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
    default void method() { // Noncompliant {{Reduce the number of returns of this method 5, down to the maximum allowed 3.}}
      if (false) return;
      if (false) return;
      if (false) return;
      if (false) return;
      return;
    }
  }
}
