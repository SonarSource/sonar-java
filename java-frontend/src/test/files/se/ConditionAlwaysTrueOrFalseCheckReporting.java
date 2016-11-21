package javax.annotation;

@interface CheckForNull {}
@interface Nullable {}

public class Class {

  void nestedCondition(boolean a) {
    if(a) {
      if(a) { // Noncompliant [[secondary=9]]
      }
    }
  }

  void relationship(boolean a, boolean b) {
    if(a < b) {
      if(b > a) { // Noncompliant should have secondary=16
      }
    }
  }

  void reassignement(boolean a, boolean b) {
    if(a) {
      b = a;
      if(b) { // Noncompliant [[secondary=24]]
      }
    }
  }


}
