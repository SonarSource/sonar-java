import java.util.List;

class B {
  B field;

  A method(A a) {
    return a;
  }
}

class A extends B {
  private static final List<A> list = new java.util.ArrayList<>();
  private static A tempVal;
  public A Instance;
  private B Instance2;
  A[] as = new A[1];

  public A() {
    list.add(this); // Noncompliant {{Make sure the use of "this" doesn't expose partially-constructed instances of this class in multi-threaded environments.}}

    tempVal = this; // Compliant
    this.tempVal = this; // Compliant
    A.tempVal = this; // Compliant

    Instance = this; // Noncompliant
    new A().Instance = this; // Noncompliant

    Instance2 = this; // Noncompliant
    super.field = this; // Noncompliant

    Instance.method(this); // Compliant
    Instance2.method(this); // Noncompliant
    tempVal = method(this); // Compliant

    as[0] = this; // Noncompliant

    new A() {
      @Override
      void notAConstructor() { }
    };
  }

  @Override
  A method(A a) {
    return a;
  }

  void notAConstructor() {
    list.add(this);
    Instance = this; // Compliant
  }
}
