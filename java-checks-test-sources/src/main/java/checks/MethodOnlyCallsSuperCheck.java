package checks;

class MethodOnlyCallsSuperCheck{
  class ParentA {
    public void f2() { }
    public int f255() { return 0; }
    public void f3() { }
    public int f4() { return 0; }
    public void f5(int i) { }
    public void f6(int a) { }
    public void f7(int a) { }
    public int f8(int a) { return 0; }
    public int f9(int a) { return 0; }
    public void f10(int a, int b) { }
    public void f11(int... a) { }
    public void f12() { }
    public void f13() { }
    public <T> void f14() {  }
    public void f15() { }
    public void f16() { }
    public <T> void f19() { }
  }

  abstract class A extends ParentA {
    abstract void f1();

    public void f2() {
    }

    public int f255() {
      return 0;
    }

    public void f3() { // Noncompliant [[sc=17;ec=19]] {{Remove this method to simply inherit it.}}
      super.f3();
    }

    public int f4() { // Noncompliant
      return super.f4();
    }

    public void f5() {
      super.f5(0);
    }

    public void f6(int a) { // Compliant, not the same method
      super.f7(a);
    }

    public void f7(int a) { // Noncompliant
      super.f7(a);
    }

    public int f8(int a) { // Noncompliant
      return super.f8(a);
    }

    public int f9(int a) {
      super.f9(a);
      return a;
    }

    public void f10(int a, int b) {
      super.f10(b, a);
    }

    public void f11(int... a) { // Noncompliant
      super.f11(a);
    }

    public void f12() {
      foo();
    }

    public void f13() {
      return;
    }

    public <T> void f14() { // Noncompliant
      super.f14();
    }

    public A() {
      super();
    }

    @SomeCrazyAnnotation
    public void f15() {
      super.f15();
    }

    @Override
    public void f16() { // Noncompliant
      super.f16();
    }

    @SomeCrazyAnnotation
    public <T> void f19() {
      super.f19();
    }

    public void foo() {
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

    protected void bar1() {}

    protected void bar2() {}

    void bar3() {}

    void bar4() {}

    public void bar5() {}
  }


  class A2 extends Parent {
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

    @Override
    void f1() {
    }
  }

  class C extends A {
    @Override
    public void bar5() { // Noncompliant
      super.bar5();
    }

    @Override
    protected void bar4() {
      super.bar4();
    }

    @Override
    void f1() {
    }
  }

  @javax.transaction.Transactional
  abstract class F extends A {
    @Override
    protected void bar2() {
      super.bar2();
    }
  }

  @org.springframework.transaction.annotation.Transactional
  abstract class G extends A {
    @Override
    protected void bar2() {
      super.bar2();
    }
  }

  @AnotherAnnotation
  abstract class H extends A {
    @Override
    protected void bar2() { // Noncompliant
      super.bar2();
    }
  }
}

class MethodOnlyCallsSuperCheckD {
  MethodOnlyCallsSuperCheckD(Object o) {}

  int foo1() { return 0; }
  int foo2() { return 0; }
  int foo3() { return 0; }
  int foo4() { return 0; }
  int foo5(Object o) { return 0; }
  static int staticMethod() { return 0; }
}

class MethodOnlyCallsSuperCheckE extends MethodOnlyCallsSuperCheckD {
  MethodOnlyCallsSuperCheckE(Object o) {
    super(o);
  }

  @Override
  int foo1() { // Compliant - throws an exception
    throw new UnsupportedOperationException();
  }

  @Override
  int foo2() { // Compliant - do not call same method
    return super.foo1();
  }

  @Override
  int foo3() { // Compliant
    return new MethodOnlyCallsSuperCheckE(new Object()).foo3();
  }

  @Override
  int foo4() { // Compliant
    return MethodOnlyCallsSuperCheckD.staticMethod();
  }

  @Override
  int foo5(Object o) { // Compliant
    return super.foo5(new Object());
  }
}

class MethodOnlyCallsSuperWithDifferentModifiers {
  static class BaseClass {
    protected void method() { }
  }
  static class StandardNoncompliantCase extends BaseClass {
    @Override
    protected void method() { // Noncompliant
      super.method();
    }
  }
  static class OverrideMethodWithFinal extends BaseClass {
    @Override
    protected final void method() { // Compliant, override to make the method final.
      super.method();
    }
  }

  static class OverrideMethodWithPublic extends BaseClass {
    @Override
    public void method() { // Compliant, override to make the method public.
      super.method();
    }
  }

  static class OverrideMethodWitSynchronized extends BaseClass {
    @Override
    protected synchronized void method() { // Compliant, override to make the method synchronized.
      super.method();
    }
  }

  static class OverrideMethodWithStrictfp extends BaseClass {
    @Override
    protected strictfp void method() { // Compliant, override to make the method strictfp.
      super.method();
    }
  }
}

@interface SomeCrazyAnnotation {
}



