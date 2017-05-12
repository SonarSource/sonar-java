import javax.annotation.CheckForNull;

/**
 * 'symbol' is passed to 'function()'
 */
abstract class A {
  void bar() {
    A a = null;                                  // flow@flow_a1 {{'a' is assigned null.}}
    // Noncompliant@+1  [[flows=flow_a1]]
    foo(a);                                      // flow@flow_a1 [[sc=9;ec=10]] {{'a' is passed to 'foo()'.}}
  }

  void qix() {
    // Noncompliant@+1  [[flows=flow_a2]]
    foo(getNullable());                          // flow@flow_a2 {{'getNullable()' can return null.}} flow@flow_a2 {{'foo()' is invoked.}}
  }

  private void foo(A a) {
    a.bar();                                     // flow@flow_a1 {{Implies 'a' is null.}} flow@flow_a1 {{'NullPointerException' is thrown here.}} flow@flow_a2 {{Implies 'a' is null.}} flow@flow_a2 {{'NullPointerException' is thrown here.}}
  }

  @CheckForNull
  abstract A getNullable();
}

/**
 * 'function()' can return _constraint_
 */
class B {
  Object field;

  void bar() {
    B b, c;
    b = foo();                                   // flow@flow_b {{'foo()' can return null.}} flow@flow_b {{'b' is assigned null.}} 
    c = b;                                       // flow@flow_b {{'c' is assigned null.}}
    // Noncompliant@+1  [[flows=flow_b]]
    c.bar();                                     // flow@flow_b {{'c' is dereferenced.}}
  }

  /**
   * method which have multiple yields having return SV with different constraints (NULL and NOT_NULL).
   */
  private B foo() {
    B result = null;
    if (field != null) {
      result = new B();
    }
    return result;
  }
}

/**
 * 'function()' returns _constraint_
 */
class C {

  void bar(Object o) {
    C c = foo(o);                                // flow@flow_c {{'foo()' returns null.}} flow@flow_c {{'c' is assigned null.}}
    // Noncompliant@+1 [[flows=flow_c]]
    if (c != null) {                             // flow@flow_c {{Expression is always false.}}
      c = new C();
    }
  }

  /**
   * method which have multiple yields but all of them having the same constraint for the returned SV.
   */
  private C foo(Object o) {
    C result;
    if (o == null) {
      result = null;
    } else {
      result = null;
    }
    return result;
  }
}

/**
 * Implies 'argName' has the same value as 'providedVarName'.
 */
abstract class D {
  void bar() {
    D param = null;                              // flow@flow_d1 {{'param' is assigned null.}}
    // Noncompliant@+1  [[flows=flow_d1]]
    foo(param);                                  // flow@flow_d1 {{'param' is passed to 'foo()'.}}
  }

  private void foo(D arg) {                      // flow@flow_d1 {{Implies 'arg' has the same value as 'param'.}}
    arg.bar();                                   // flow@flow_d1 {{Implies 'arg' is null.}} flow@flow_d1 {{'NullPointerException' is thrown here.}}
  }

  void qix(D param) {
    tst(param);                                  // flow@flow_d2 [[order=1]] {{'param' is passed to 'tst()'.}} flow@flow_d2 [[order=4]] {{Implies 'param' is null.}}
    // Noncompliant@+1  [[flows=flow_d2]]
    gul(param);                                  // flow@flow_d2 [[order=5]] {{'param' is passed to 'gul()'.}}
  }

  private void tst(D d) {                        // flow@flow_d2 [[order=2]] {{Implies 'd' has the same value as 'param'.}}
    if (d == null) {                             // flow@flow_d2 [[order=3]] {{Implies 'd' is null.}}
      doSomething();
    }
  }

  private void gul(D arg) {                      // flow@flow_d2 [[order=6]] {{Implies 'arg' has the same value as 'param'.}}
    arg.bar();                                   // flow@flow_d2 [[order=7]] {{Implies 'arg' is null.}} flow@flow_d2 [[order=8]] {{'NullPointerException' is thrown here.}}
  }

  abstract void doSomething();
}

class ZeroConstraint {

  private int maybeZero() {
    if (b) {
      return 0;
    }
    return 1;
  }

  private int zero() {
    return 0;
  }

  void t1() {
    int i = maybeZero(); // flow@zero1 {{'maybeZero()' can return zero.}} flow@zero1 {{'i' is assigned zero.}}
    1 / i; // Noncompliant [[flows=zero1]] flow@zero1
  }

  void t() {
    int i = zero(); // flow@zero2 {{'zero()' returns zero.}} flow@zero2 {{'i' is assigned zero.}}
    1 / i; // Noncompliant [[flows=zero2]] flow@zero2
  }
}

class BooleanConstraint {

  private boolean sure() {
    return false;
  }

  void f() {
    boolean b = sure(); // flow@bool {{'sure()' returns false.}} flow@bool {{'sure()' returns non-null.}} flow@bool {{'b' is assigned false.}} flow@bool {{'b' is assigned non-null.}}
    if (b); // Noncompliant [[flows=bool]] flow@bool
  }
}
