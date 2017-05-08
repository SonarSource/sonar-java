class A {
  private int foo(boolean a) {
    int b = 12;
    if(a) {
      return b;
    }
    return b;  // Noncompliant
  }

  private int foo2(boolean a) {
    int b = 12;
    if(a) {
      return b;
    }
    return b - 1;
  }
  int foo3(boolean a) {
    int b = 12;
    if(a) {
      return b;
    }
    return b;  // false negative : caching of program states because of liveness of params.
  }

  private String foo4(boolean a) {
    String b = "foo";
    if(a) {
      return b;
    }
    return b; // Noncompliant
  }

  void plop() {
    a;
  }

  private int doSomething() {
    System.out.println("");
    return 42;
  }
  private int getConstant2() {
    return 42;
  }

  private A() {}

  String constructComponentName() {
    synchronized (getClass()) {
      return base + nameCounter++;
    }
  }

  java.util.List<String> f() {
    System.out.println("");
    if(bool) {
      System.out.println("");
    }
    return new ArrayList<String>();
  }
  java.util.Map<String> f() {
    java.util.Map<String> foo = new java.util.HashMap<String>();
    if(bool) {
      return foo;
    }
    return foo;
  }

}

