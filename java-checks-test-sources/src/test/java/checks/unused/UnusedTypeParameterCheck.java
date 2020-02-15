package checks.unused;

class UnusedTypeParameterCheck<T, S> { // Noncompliant [[sc=35;ec=36]] {{S is not used in the class.}}
  T field;
  <W,X> void fun(X x) {} // Noncompliant {{W is not used in the method.}}
}
interface UnusedTypeParameterCheckB<U, V> { // Noncompliant {{V is not used in the interface.}}
  U foo();
}
class UnusedTypeParameterCheckC {
  void fun(){}
}
