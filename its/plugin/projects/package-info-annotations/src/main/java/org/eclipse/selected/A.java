package org.eclipse.selected;

// package annotated with @NonNullByDefault on PARAMETER and RETURN_TYPE
public abstract class A {

  void foo(A a) {
    if (a != null // java:S2589 - condition always true
      && a.getSomething() == null) { // java:S2589 - condition always false
      doSomething();
    }
  }

  abstract Object getSomething();

  abstract void doSomething();
}
