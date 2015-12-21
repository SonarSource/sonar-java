class A {
  public void f() {
  }

  public void f(int a) {
    a = 0; // Noncompliant [[sc=5;ec=6]] {{Introduce a new variable instead of reusing the parameter "a".}}
    a += 1; // Noncompliant {{Introduce a new variable instead of reusing the parameter "a".}}
    int b = a;

    try {
    } catch (Exception e) {
      e = new RuntimeException(); // Noncompliant

      int b = 0;
      b = 0;
    }

    int e;
    e = 0;
    this.a = 0;
  }

  public void f(int[] a) {
    a[0] = 0;
  }

  public A(int field) {
    field = field; // Noncompliant
  }

  public void f(int a) {
    a++; // Noncompliant
    ++a; // Noncompliant
    a--; // Noncompliant
    --a; // Noncompliant
    !a;
    ~a;
    int b = 0;
    b++;
    this.a++;
  }
  @Annotation(param="value") //raise issue because this param is considered as a reassignement of method parameter.
  void foo(String param) {}
}
