import javax.annotation.CheckForNull;

/**
 * 'symbol' is passed to 'function()'
 */
abstract class A {
  void bar() {
    A a = null;                                  // flow@flow_a1 {{Implies 'a' is null.}}
    // Noncompliant@+1  [[flows=flow_a1]]
    foo(a);                                      // flow@flow_a1 [[sc=9;ec=10]] {{'a' is passed to 'foo()'.}}
  }

  void qix() {
    // Noncompliant@+1  [[flows=flow_a2]]
    foo(getNullable());                          // flow@flow_a2 {{'getNullable()' can return null.}} flow@flow_a2 {{'foo()' is invoked.}}
  }

  private void foo(A a) {
    a.bar();                                     // flow@flow_a1 {{Implies 'a' is null.}} flow@flow_a1 {{'NullPointerException' is thrown.}} flow@flow_a2 {{Implies 'a' is null.}} flow@flow_a2 {{'NullPointerException' is thrown.}}
  }

  @CheckForNull
  abstract A getNullable();
}

/**
 * 'function()' can return _constraint_
 */
class B {
  Object field;

  /**
   * method which have multiple yields having return SV with different constraints (NULL and NOT_NULL).
   */
  private B foo() {
    B result = null; // flow@flow_b {{Implies 'result' is null.}}
    if (field != null) {
      result = new B();
    }
    return result;
  }

  void bar() {
    B b, c;
    b = foo();                                   // flow@flow_b {{'foo()' can return null.}} flow@flow_b {{Implies 'b' can be null.}}
    c = b;                                       // flow@flow_b {{Implies 'c' has the same value as 'b'.}}
    // Noncompliant@+1  [[flows=flow_b]]
    c.bar();                                     // flow@flow_b {{'c' is dereferenced.}}
  }
}

/**
 * 'function()' returns _constraint_
 */
class C {

  /**
   * method which have multiple yields but all of them having the same constraint for the returned SV.
   */
  private C foo(Object o) {
    C result;
    if (o == null) {
      result = null; // flow is missing because yield is reduced
    } else {
      result = null;
    }
    return result;
  }

  void bar(Object o) {
    C c = foo(o);                                // flow@flow_c1 {{'foo()' returns null.}} flow@flow_c1 {{Implies 'c' is null.}}
    // Noncompliant@+1 [[flows=flow_c1]]
    if (c != null) {                             // flow@flow_c1 {{Expression is always false.}}
      c = new C();
    }
  }

}

/**
 * Implies 'argName' has the same value as 'providedVarName'.
 */
abstract class D {
  void bar() {
    D param = null;                              // flow@flow_d1 {{Implies 'param' is null.}}
    // Noncompliant@+1  [[flows=flow_d1]]
    foo(param);                                  // flow@flow_d1 {{'param' is passed to 'foo()'.}}
  }

  private void foo(D arg) {                      // flow@flow_d1 {{Implies 'arg' has the same value as 'param'.}}
    arg.bar();                                   // flow@flow_d1 {{Implies 'arg' is null.}}  flow@flow_d1 {{'NullPointerException' is thrown.}}
  }

  void qix(D param) {
    tst(param);   // comparing argument to null will not result in exploring param with NULL constraint
    gul(param);
  }

  private void tst(D d) {
    if (d == null) {
      doSomething();
    }
  }

  private void gul(D arg) {
    arg.bar();
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
    int i = maybeZero(); // flow@zero1 {{'maybeZero()' can return zero.}} flow@zero1 {{Implies 'i' can be zero.}}
    int j = 1 / i; // Noncompliant [[flows=zero1]] flow@zero1
  }

  void t() {
    int i = zero(); // flow@zero2 {{'zero()' returns zero.}} flow@zero2 {{Implies 'i' is zero.}}
    int j = 1 / i; // Noncompliant [[flows=zero2]] flow@zero2
  }
}

class BooleanConstraint {

  private boolean sure() {
    return false;
  }

  void f() {
    boolean b = sure(); // flow@bool {{'sure()' returns false.}} flow@bool {{Implies 'b' is false.}}
    if (b); // Noncompliant [[flows=bool]] flow@bool
  }
}

class FollowingReturnValue {
  private Object f() {
    Object a = new Object(); // flow@caf {{Constructor implies 'not null'.}} flow@caf {{Implies 'a' is not null.}}
    Object o = a; // flow@caf {{Implies 'o' has the same value as 'a'.}}
    return o;
  }

  void cat() {
    Object o = f(); // flow@caf {{'f()' returns not null.}} flow@caf {{Implies 'o' is not null.}}
    // Noncompliant@+1 [[flows=caf]]
    if (o == null) { // flow@caf {{Expression is always false.}}
      o.toString();
    }
  }
}
