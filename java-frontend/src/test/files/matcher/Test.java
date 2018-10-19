import java.lang.Boolean;
import java.lang.Integer;

class Test extends Object {

  public String toString() {
    return "Test";
  }

  public String toString(int param) {
    return toString(String.valueOf(param));
  }

  public String toString(String param) {
    return param;
  }

  public String mit() {
    return new Integer(5).toString();
  }

  public InnerClass inner() {
    return this.new InnerClass();
  }

  static class InnerClass {
    public String toString() {
      return "InnerClass";
    }
    public void coverage(UnknownClass unknownClass) {
      unknownClass.unknownMethod();
    }
  }

  public int foo() {
    return foo();
  }
  void test() {
    java.util.function.Supplier<String> s1 = this::toString;
    java.util.function.Supplier<String> s2 = Object::toString;
  }
}
