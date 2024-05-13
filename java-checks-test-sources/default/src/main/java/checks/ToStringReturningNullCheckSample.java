package checks;

class ToStringReturningNullCheckSample {
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

class ToStringReturningNullCheckSampleB {
  public String toString() {
    return null; // Noncompliant {{Return empty string instead.}}
  }  
}

class ToStringReturningNullCheckSampleC {
  public String toString() {
    return (null); // Noncompliant
//          ^^^^
  }
}

class ToStringReturningNullCheckSampleD {
  protected Object clone() {
    return null; // Noncompliant {{Return a non null object.}} [[quickfixes=!]]
//         ^^^^
  }
}

class QuickFixesA{
  public String toString() {
    return (null); // Noncompliant [[quickfixes=qf1]]
//          ^^^^
    // fix@qf1 {{Replace null with an empty string}}
    // edit@qf1 [[sc=12;ec=18]] {{""}}
  }
}
class QuickFixesB{
  public String toString() {
    return null; // Noncompliant [[quickfixes=qf2]]
//         ^^^^
    // fix@qf2 {{Replace null with an empty string}}
    // edit@qf2 [[sc=12;ec=16]] {{""}}
  }
}
class QuickFixesC{
  public String toString() {
    if(someCondition()) {
      return null; // Noncompliant [[quickfixes=qf3]]
//           ^^^^
      // fix@qf3 {{Replace null with an empty string}}
      // edit@qf3 [[sc=14;ec=18]] {{""}}
    }
    return "";
  }
  boolean someCondition() {return false;}
}
