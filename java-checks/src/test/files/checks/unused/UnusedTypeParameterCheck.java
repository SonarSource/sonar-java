class A<T, S> { // Noncompliant [[sc=12;ec=13]] {{S is not used in the class.}}
  T field;
  <W,X> void fun(X x) {} // Noncompliant {{W is not used in the method.}}
}
interface B<U, V> { // Noncompliant {{V is not used in the interface.}}
  U foo();
}
class C {
  void fun(){}
}
