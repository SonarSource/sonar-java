package javax.annotation;

@interface CheckForNull {}
@interface Nullable {}

public class Class {

  void nestedCondition(boolean a) {
    if(a) { // flow@nested {{Implies 'a' is true.}}
      if(a) { // Noncompliant [[flows=nested]] flow@nested {{Expression is always true.}}
      }
    }
  }

  void relationship(int a, int b) {
    if(a < b) { // flow@rel {{Implies 'b' is true.}}  FIXME -- name of symbol is wrong here, should be whole expression
      if(b > a) { // Noncompliant [[flows=rel]] flow@rel {{Expression is always true.}}
      }
    }
  }

  void reassignement(boolean a, boolean b) {
    if(a) { // flow@reass {{Implies 'a' is true.}}
      b = a; // flow@reass {{Implies 'b' has the same value as 'a'.}}
      if(b) { // Noncompliant [[flows=reass]] flow@reass {{Expression is always true.}}
      }
    }
  }

  void unarySymbolicvalue(boolean a, boolean b) {
    if(! (a ==b )) // FIXME missing flow message unary SV
      if (a == b); // Noncompliant FIXME@unarySv Expression is always false.
  }
  void unary(boolean a) {
    if(!a) // flow@unary {{Implies 'a' is false.}}
      if (a); // Noncompliant [[flows=unary]] flow@unary {{Expression is always false.}}
  }
  void reporting() {
    boolean a = true;
    boolean b = true;
    if (unknown() && a // Noncompliant [[sc=22;ec=23]]
      && b) { // Noncompliant [[sc=10;ec=11]]
    }

    if (unknown() && a) { // Noncompliant [[sc=22;ec=23]]
    }
  }

}
