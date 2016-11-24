package javax.annotation;

@interface CheckForNull {}
@interface Nullable {}

public class Class {

  void nestedCondition(boolean a) {
    if(a) { // flow@nested {{}}
      if(a) { // Noncompliant [[flows=nested]]
      }
    }
  }

  void relationship(boolean a, boolean b) {
    if(a < b) { // flow@rel {{}}
      if(b > a) { // Noncompliant [[flows=rel]]
      }
    }
  }

  void reassignement(boolean a, boolean b) {
    if(a) { // flow@reass {{}}
      b = a; // flow@reass {{}}
      if(b) { // Noncompliant [[flows=reass]]
      }
    }
  }

  void unarySymbolicvalue(boolean a, boolean b) {
    if(! (a ==b )) // flow@unarySv {{}}
      if (a == b); // Noncompliant [[flows=unarySv]]
  }
  void unary(boolean a) {
    if(!a) // flow@unary {{}}
      if (a); // Noncompliant [[flows=unary]]
  }


}
