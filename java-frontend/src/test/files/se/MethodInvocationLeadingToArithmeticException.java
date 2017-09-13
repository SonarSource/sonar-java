class A {
  void usual() {
    int j = 0;
    int k = 42 / j; // Noncompliant {{Make sure "j" can't be zero before doing this division.}}
  }

  void foo1() {
    divByZeroIfArg1Zero(42, 7); // Compliant
  }

  void foo2() {
    A.divByZeroIfArg1Zero(42, 0); // Noncompliant [[flows=foo2]] {{A division by zero will occur when invoking method "divByZeroIfArg1Zero()".}} flow@foo2 [[order=1]] {{'divByZeroIfArg1Zero()' is invoked.}}
  }

  void foo3(int j) {
    int i = 42; // flow@foo3 [[order=1]] {{Implies 'i' is non-zero.}}
    if (j == 0) { // flow@foo3 [[order=2]] {{Implies 'j' is zero.}}
      divByZeroIfArg1Zero(i, j); // Noncompliant [[flows=foo3]] {{A division by zero will occur when invoking method "divByZeroIfArg1Zero()".}} flow@foo3 [[order=3;sc=30;ec=31]] {{'j' is passed to 'divByZeroIfArg1Zero()'.}}
    }
  }

  void yield(int j) {
    divByZeroIfArg1Zero(42, j);
    if (j == 0) { // we know from yield that j!=0
      divByZeroIfArg1Zero(42, j); // unreachable
    }
  }

  void foo4() {
    int i = 42;
    int j = 0;
    try {
      divByZeroIfArg1Zero(i, j); // Compliant - catched
    } catch (ArithmeticException e) {
      i = 7;
    }
    divByZeroIfArg1Zero(i, j); // Noncompliant {{A division by zero will occur when invoking method "divByZeroIfArg1Zero()".}}
  }

  void foo5() {
    int i = 42;
    int j = 0;
    try {
      throwsExceptionIfArg1Zero(i, j); // Compliant - not triggered by divizion by zero
    } catch (ArithmeticException e) {
      i = 7;
    }
    throwsExceptionIfArg1Zero(i, j); // Compliant
  }

  void foo6() {
    int i = 42;
    int j = 0;
    try {
      divByZeroIfArg1Zero(i, j); // Noncompliant {{A division by zero will occur when invoking method "divByZeroIfArg1Zero()".}}
    } catch (MyCheckedException e) {
      i = 7;
    }
    divByZeroIfZero(i, j); // Compliant - can not be reached
  }

  void foo7() {
    divByZeroIfZero(42); // Compliant
    divByZeroIfZero(0); // Noncompliant {{A division by zero will occur when invoking method "divByZeroIfZero()".}}
  }

  static int divByZeroIfZero(int i) {
    if (i == 0) {
      return 7 / i; // Noncompliant {{Make sure "i" can't be zero before doing this division.}}
    }
    return 42;
  }

  static int divByZeroIfArg1Zero(int i, int j) {
    return i / j; // flow@foo2 [[order=2]] {{Implies 'j' is zero.}} flow@foo2 [[order=4]] {{'ArithmeticException' is thrown.}} flow@foo3 [[order=4]] {{Implies 'j' is zero.}} flow@foo3 [[order=5]] {{'ArithmeticException' is thrown.}}
  }

  static int throwsExceptionIfArg1Zero(int i, int j) {
    if (j == 0) {
      throw new ArithmeticException();
    }
    return i / j;
  }

  static class MyCheckedException extends Exception { }
}
