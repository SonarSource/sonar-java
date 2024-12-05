package checks.S3252_StaticMemberAccessCheckSample;

public class S3252TestDefaultPackage {

  static class A {
    public static final int CONSTANT = 42;

  }

  static class B extends A {

    public static void foo()  {
      int x = B.CONSTANT; // Noncompliant
    }
  }
}
