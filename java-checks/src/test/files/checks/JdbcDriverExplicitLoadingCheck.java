
class A {
  void doStuff() {
    Class.forName("org.h2.Driver"); // Noncompliant [[sc=11;ec=18]]
  }
}

class B {
  void doStuff() {
    Class.forName("java.lang.String");
  }
}

class C {
  private static final String JDBC_DRIVER = "org.h2.Driver";
  void doStuff() {
    Class.forName(JDBC_DRIVER); // Noncompliant
  }
}

class D {
  private static final String CLASS_NAME = "java.lang.String";
  void doStuff() {
    Class.forName(CLASS_NAME);
  }
}
