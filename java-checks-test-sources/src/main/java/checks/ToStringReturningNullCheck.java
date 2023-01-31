package checks;

class ToStringReturningNullCheck {
  public String toString() {
    return "";
  }
  public String toString(int x) {
    return null;
  }
  public String notToString() {
    return null;
  }
}

class ToStringReturningNullCheckB {
  public String toString() {
    return null; // Noncompliant {{Return empty string instead.}}
  }  
}

class ToStringReturningNullCheckC {
  public String toString() {
    return (null); // Noncompliant [[sc=13;ec=17]]
  }
}

class ToStringReturningNullCheckD {
  protected Object clone() {
    return null; // Noncompliant [[sc=12;ec=16]] {{Return a non null object.}}
  }
}

class QuickFixes{
  public String toString() {
    return (null); // Noncompliant [[sc=13;ec=17;quickfixes=qf1]]
    // fix@qf1 {{Replace null with an empty string}}
    // edit@qf1 [[sc=12;ec=18]] {{""}}
  }
}
class QuickFixes2{
  public String toString() {
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=qf2]]
    // fix@qf2 {{Replace null with an empty string}}
    // edit@qf2 [[sc=12;ec=16]] {{""}}
  }
}
