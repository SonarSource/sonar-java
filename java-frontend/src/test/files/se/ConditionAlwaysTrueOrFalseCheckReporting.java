package javax.annotation;

@interface CheckForNull {}
@interface Nullable {}

public class Class {

  void nestedCondition(boolean a) {
    if(a) { // flow@nested {{...}}
      if(a) { // Noncompliant [[flows=nested]] flow@nested {{Condition is always true}}
      }
    }
  }

  void relationship(boolean a, boolean b) {
    if(a < b) { // flow@rel {{...}}
      if(b > a) { // Noncompliant [[flows=rel]] flow@rel {{Condition is always true}}
      }
    }
  }

  void reassignement(boolean a, boolean b) {
    if(a) { // flow@reass {{...}}
      b = a; // flow@reass {{...}}
      if(b) { // Noncompliant [[flows=reass]] flow@reass {{Condition is always true}}
      }
    }
  }

  void unarySymbolicvalue(boolean a, boolean b) {
    if(! (a ==b )) // flow@unarySv {{...}}
      if (a == b); // Noncompliant [[flows=unarySv]] flow@unarySv {{Condition is always false}}
  }
  void unary(boolean a) {
    if(!a) // flow@unary {{...}}
      if (a); // Noncompliant [[flows=unary]] flow@unary {{Condition is always false}}
  }


}
