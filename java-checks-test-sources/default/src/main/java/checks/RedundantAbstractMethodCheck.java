package checks;

class RedundantAbstractMethodCheck {
  interface I {
    void foo();
  }

  abstract static class A implements I {
    @Override public abstract void foo(); // Noncompliant {{"foo" is defined in the "I" interface and can be removed from this class.}}
    abstract void bar();
  }

  abstract static class B implements I {
    @Override public void foo() {} // Compliant
  }

  abstract static class C implements I {
    abstract void bar();
  }

  abstract static class D extends C {
    @Override public abstract void foo(); // Noncompliant {{"foo" is defined in the "I" interface and can be removed from this class.}}
    @Override abstract void bar();
  }

  static class E implements I {
    @Override public void foo() {}
  }
}


class ParametrizedMethod {
  interface I {
    <T> T convert(A<T> arg);
  }

  static class A<X> {}

  abstract static class B implements I {
    @Override public abstract Object convert(A arg); // Compliant - parametrized aspect of the method has been removed
  }

  abstract static class C implements I {
    @Override abstract public <T> T convert(A<T> arg); // Noncompliant
  }
}

class ReturnTypeChanged {
  interface I<X> {
    I<X> returnValue();
  }

  abstract class A<Y> implements I<Y> {
    @Override
    abstract public A<Y> returnValue(); // Compliant
  }
}

class SpecilizationClass {
  @interface MyAnnotation {}
  static class MyException extends Exception {}

  interface I {
    A foo();
    @MyAnnotation Object bar(A a);
    Object qix(C<A> c);
    C<A> gul();
    int size();
    void bom(A a);
    void dom(@MyAnnotation A a);
    void throwing() throws MyException;
  }

  static class A {}
  static class B extends A {}
  static class C<X> {}

  abstract static class D implements I {
    @Override public abstract B foo(); // Compliant - return B instead of A
    @Override public abstract Object qix(C c); // Compliant - use raw type
    @Override public abstract C gul(); // Compliant - use raw type as return type
    @Override public abstract void bom(@MyAnnotation A a); // Compliant
    @Override @MyAnnotation @Deprecated public abstract int size(); // Compliant - extra 'deprecated' annotation
    @Override public abstract void throwing(); // Compliant - removed thrown expression
  }

  abstract static class E implements I {
    @Override public abstract A foo(); // Noncompliant
    @Override public abstract Object qix(C<A> c); // Noncompliant
    @Override public abstract Object bar(A a); // Noncompliant
    @Override public abstract C<A> gul(); // Noncompliant
    @Override public abstract int size(); // Noncompliant
    @Override public abstract void dom(@MyAnnotation A a); // Noncompliant
    @Override public abstract void throwing() throws MyException; // Noncompliant
  }

  abstract static class F implements I {
    @Override @MyAnnotation public abstract Object bar(A a); // Noncompliant
  }
}
