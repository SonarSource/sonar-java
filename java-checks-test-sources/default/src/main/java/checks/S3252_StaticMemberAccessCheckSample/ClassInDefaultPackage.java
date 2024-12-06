public class ClassInDefaultPackage {

  static class A {
    public static final int CONSTANT = 42;

  }

  static class B extends A {

    public static void foo()  {
      int x = B.CONSTANT; // Noncompliant
    }
  }
}
