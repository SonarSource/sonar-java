package symbolicexecution.checks;

@interface CheckForNull {}
@interface Nullable {}

public class ConditionAlwaysTrueOrFalseCheckReporting {

  void nestedCondition(boolean a) {
    if(a) { // flow@nested {{Implies 'a' is true.}}
      if(a) { // Noncompliant {{Expression is always true.}}
      }
    }
  }

  void relationship(int a, int b) {
    if(a < b) { // flow@rel {{Implies 'b' is true.}}  FIXME -- name of symbol is wrong here, should be whole expression
      if(b > a) { // Noncompliant {{Expression is always true.}}
      }
    }
  }

  void reassignement(boolean a, boolean b) {
    if(a) { // flow@reass {{Implies 'a' is true.}}
      b = a; // flow@reass {{Implies 'b' has the same value as 'a'.}}
      if(b) { // Noncompliant {{Expression is always true.}}
      }
    }
  }

  void unarySymbolicvalue(boolean a, boolean b) {
    if(! (a ==b )) // FIXME missing flow message unary SV
      if (a == b); // Noncompliant
  }
  void unary(boolean a) {
    if(!a) // flow@unary {{Implies 'a' is false.}}
      if (a); // Noncompliant {{Expression is always false.}}
  }
  void reporting() {
    boolean a = true;
    boolean b = true;
    if (unknown() && a && b) {  // Compliant, method contains unknown symbols
    }

    if (unknown() && a) {  // Compliant, method contains unknown symbols
    }
  }

}
