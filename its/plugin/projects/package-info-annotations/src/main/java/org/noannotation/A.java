package org.noannotation;

// package not annotated - constraints are learned along the way
public abstract class A {

  void foo(A a) {
    a.doSomething();
    if (a != null) { // java:S2589 - condition always true
      a.doSomething();
    }
  }

  abstract void doSomething();
}
