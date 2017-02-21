class A {
  private abstract void f();

  private void f() {
  }

  private void f() {
    return 0;
  }

  private void f() { // Noncompliant [[sc=16;ec=17]] {{Remove this method to simply inherit it.}}
    super.f();
  }

  private void f() { // Noncompliant
    return super.f();
  }

  private void f() {
    super.f(0);
  }

  private void f(int a) {
    super.f();
  }

  private void f(int a) { // Noncompliant
    super.f(a);
  }

  private int f(int a) { // Noncompliant
    return super.f(a);
  }

  private int f(int a) {
    super.f(a);
    return a;
  }

  private void f(int a, int b) {
    super.f(b, a);
  }

  private void f(int... a) { // Noncompliant
    super.f(a);
  }

  private void f() {
    foo();
  }

  private void f() {
    return;
  }

  private <T> void f() { // Noncompliant
    super.f();
  }

  public A() {
    super();
  }

  @SomeCrazyAnnotation
  private void f() {
    super.f();
  }

  @Override
  public void f() { // Noncompliant
    super.f();
  }

  @Override
  @SomeCrazyAnnotation
  private void f() {
    super.f();
  }

  @SomeCrazyAnnotation
  @Override
  private void f() {
    super.f();
  }

  @SomeCrazyAnnotation
  private <T> void f() {
    super.f();
  }

  @foo.Deprecated
  private <T> void f() {
    super.f();
  }

  @Override
  @Override
  private <T> void f() { // Noncompliant
    super.f();
  }


  @Override
  public boolean equals(Object obj) { // Compliant
    return super.equals(obj);
  }

  @Override
  public int hashCode() { // Compliant
    return super.hashCode();
  }

  @Override
  public String toString() { // Compliant
    return super.toString();
  }

  @Override
  public final boolean equals(Object obj) { //Compliant, equals is final
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final String toString() {
    return super.toString();
  }

  protected void bar1() {}

  protected void bar2() {}

  void bar3() {}

  void bar4() {}

  public void bar5() {}
}

class B extends A {
  @Override
  public void bar1() { // Compliant
    super.bar1();
  }

  @Override
  protected void bar2() { // Noncompliant
    super.bar2();
  }

  @Override
  public void bar3() { // Compliant
    super.bar3();
  }

  @Override
  void bar4() { // Noncompliant
    super.bar4();
  }
}

class C extends A {
  @Override
  void bar2() { // Compliant (but does not compile... can not reduce visibility [protected -> package])
    super.bar2();
  }

  @Override
  public void bar5() { // Noncompliant
    super.bar5();
  }

  @Override
  protected void bar4() {
    super.bar4();
  }
}
public class finalMethodsExclusion {
  static class BaseClass {
    public void methodA() { }
    public void methodB() { }
  }
  static class OverrideClass extends BaseClass {
    public final void methodA() { // compliant : override to make the method final.
      super.methodA();
    }
    public void methodB() { // Noncompliant
      super.methodB();
    }
  }
}

class D {
  D(Object o) {}

  int foo1() { return 0; }
  int foo2() { return 0; }
  int foo3() { return 0; }
  int foo4() { return 0; }
  int foo5(Object o) { return 0; }
  static int staticMethod() { return 0; }
}

class E extends D {
  E(Object o) {
    super(o);
  }

  @Override
  int foo1() { // Compliant - throws an exception
    throw new UnsupportedOperationException();
  }

  @Override
  int foo2() { // Compliant - do not call same method
    return super.foo();
  }

  @Override
  int foo3() { // Compliant
    return new D().foo3();
  }

  @Override
  int foo4() { // Compliant
    return D.staticMethod();
  }

  @Override
  int foo5(Object o) { // Compliant
    return super.foo5(new Object());
  }
}
