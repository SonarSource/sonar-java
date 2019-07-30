class A {
  public void f() {
  }
  abstract void method();
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
    field = field; // compliant
  }

  public void f(int a) {
    a++; // compliant
    ++a; // compliant
    a--; // compliant
    --a; // compliant
    int b = 0;
    b++;
    this.a++;
  }
  @Annotation(param="value") //raise issue because this param is considered as a reassignement of method parameter.
  void foo(String param) {}

  void meth() {
    try {
    } catch (Exception e) {
      e = new RuntimeException(); // Noncompliant reassigned before read.
      throw e;
    }
    while (someBool) {
      try {
      } catch (Exception e) {
        e.printStackTrace();
        e = new RuntimeException();
        break;
      }
    }
  }

  void forLoops(java.util.List<String> list) {
    for (String s : list) {
      s = ""; // Noncompliant
    }
    for (String s1 : list) {
      System.out.println(s1);
      s1 = ""; // compliant
    }
    for (String s : list) {
      Object o = () -> new Object();
      s = ""; // Noncompliant
    }
    for (String s3 : list) {
      Object o = () -> s3.length();
      s3 = ""; // compliant
    }
  }

  Object o = () -> {
    try {
      return null;
    } catch (Exception e) {
      e = new Exception(); // false negative, not defined within a method
      throw e;
    }
  };
}
