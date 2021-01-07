package org.javax.nonnullparameters;

// package annotated with @ParametersAreNonnullByDefault
public abstract class A {

  void foo(A a) {
    if (a != null) { // java:S2589 - condition always true
      a.doSomething();
    }
  }

  abstract void doSomething();
}
