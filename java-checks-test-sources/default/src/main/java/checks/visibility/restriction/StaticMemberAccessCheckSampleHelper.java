package checks.visibility.restriction;

public class StaticMemberAccessCheckSampleHelper {

  class A {
    public static final int CONSTANT = 42;
  }

  public class B  extends A {
  }

  public class Foo {
    public static final int CONSTANT = 42;
  }

  public class Bar  extends Foo {
  }


}
