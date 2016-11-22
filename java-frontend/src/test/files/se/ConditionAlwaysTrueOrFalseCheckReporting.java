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
      if(b > a) { // Noncompliant [[secondary=16]]
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

  void unarySymbolicvalue(boolean a, boolean b) {
    if(! (a ==b ))
      if (a == b); // Noncompliant [[secondary=31]]
  }
  void unary(boolean a) {
    if(!a)
      if (a); // Noncompliant [[secondary=35]]
  }


}
