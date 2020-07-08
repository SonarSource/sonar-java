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

  int intMember;

  @Override
  public boolean equals(Object other) { // Compliant because equals method is excempt from the rule
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    A other = (A) obj;
    return intMember == other.intMember && Objects.equals(stringMember, other.stringMember);
  }

  public boolean equals(A other) { // Noncompliant because this is not a proper equals method
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    A other = (A) obj;
    return intMember == other.intMember && Objects.equals(stringMember, other.stringMember);
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
