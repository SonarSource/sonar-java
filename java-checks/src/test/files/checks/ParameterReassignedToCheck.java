class A {
  public void f() {
  }

  public void f(int a) {
    a = 0; // Noncompliant
    a += 1; // Noncompliant
    int b = a; // Compliant

    try {
    } catch (Exception e) {
      e = new RuntimeException(); // Noncompliant

      int b = 0;
      b = 0; // Compliant
    }

    int e;
    e = 0; // Compliant
    this.a = 0; // Compliant
  }

  public void f(int[] a) {
    a[0] = 0; // Compliant
  }

  public A(int field) {
    field = field; // Noncompliant
  }

  public void f(int a) {
    a++; // Noncompliant
    ++a; // Noncompliant
    a--; // Noncompliant
    --a; // Noncompliant
    !a; // Compliant
    ~a; // Compliant
    int b = 0;
    b++; // Compliant
    this.a++; // Compliant
  }
  @Annotation(param="value") //raise issue because this param is considered as a reassignement of method parameter.
  void foo(String param) {}
}
