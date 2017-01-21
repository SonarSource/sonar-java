package javax.annotation;

@interface CheckForNull {}
@interface Nullable {}

public class Class {

  void nestedCondition(boolean a) {
    if(a) { // flow@nested {{Implies 'a' is true}}
      if(a) { // Noncompliant [[flows=nested]] flow@nested {{Condition is always true}}
      }
    }
  }

  void relationship(int a, int b) {
    if(a < b) { // flow@rel {{Implies 'b' is "maybe-zero"}}  - contributed by DivByZeroCheck, see SONARJAVA-2045
      if(b > a) { // Noncompliant [[flows=rel]] flow@rel {{Condition is always true}}
      }
    }
  }

  void reassignement(boolean a, boolean b) {
    if(a) { // flow@reass {{Implies 'a' is true}}
      b = a; // flow@reass {{'b' is assigned true}}
      if(b) { // Noncompliant [[flows=reass]] flow@reass {{Condition is always true}}
      }
    }
  }

  void unarySymbolicvalue(boolean a, boolean b) {
    if(! (a ==b )) // FIXME missing flow message unary SV
      if (a == b); // Noncompliant [[flows=unarySv]] flow@unarySv {{Condition is always false}}
  }
  void unary(boolean a) {
    if(!a) // flow@unary {{Implies 'a' is false}}
      if (a); // Noncompliant [[flows=unary]] flow@unary {{Condition is always false}}
  }


}
