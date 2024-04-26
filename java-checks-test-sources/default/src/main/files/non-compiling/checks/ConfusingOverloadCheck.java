package checks;

class ConfusingOverloadCheck {
  public class SomeParent {
    public static void staticDifference(int i) {
      return -1;
    }
  }

  public class SomeChild extends SomeParent {
    // Signature overloading of a static method does not compile
    public void staticDifference(int i) { // Noncompliant {{Rename this method or make it "static".}}
      return -1;
    }
  }

  public class Child3 extends ParentUnkown {
    public void doSomething(Computer.Pear p) {
    }
  }

  class UnknownParam {
    private static int method(UNKNOWN arg) {
      return -1;
    }
  }

  class UnknownParamChild extends UnknownParam {
    protected int method(UNKNOWN arg) {
      return -2;
    }
  }
}
