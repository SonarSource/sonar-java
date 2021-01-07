package org.javax.nullparameters;

// package annotated with @ParametersAreNullableByDefault
public abstract class A {

  void foo(A a) {
    a.doSomething(); // java:S2259 - NPE: a is nullable
  }

  abstract void doSomething();
}
