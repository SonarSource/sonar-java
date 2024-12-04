package checks.visibility.restriction;

public class StaticMemberAccessCheckSampleHelper {

  static class A {
    public static final int CONSTANT = 42;
  }

  static public class B  extends A {
  }

  static public class Foo {
    public static final int CONSTANT = 42;
  }

  static public class Bar  extends Foo {
  }


}
