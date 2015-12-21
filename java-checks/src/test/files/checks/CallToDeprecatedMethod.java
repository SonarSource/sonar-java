import java.lang.Deprecated;

public class CallToDeprecatedMethodCheck {

  public CallToDeprecatedMethodCheck() {
    String string = new String("my string");
    string.getBytes(1, 1, new byte[3], 7); // Noncompliant [[sc=12;ec=20]] {{Remove this use of "getBytes"; it is deprecated.}}
    new DeprecatedConstructor(); // Noncompliant [[sc=9;ec=30]] {{Remove this use of "DeprecatedConstructor"; it is deprecated.}}
    new MyDeprecatedClass(); // Noncompliant
    old++; // Noncompliant
    MyDeprecatedClass.a++; // Noncompliant
  }

  @Deprecated
  int old;

  @Deprecated
  private static class MyDeprecatedClass {
    static int a;
  }

  private static class DeprecatedConstructor {
    @Deprecated
    public DeprecatedConstructor() {
      string.getBytes(1, 1, new byte[3], 7);
    }
    Object a  = "".getBytes(1, 1, new byte[3], 7); // Noncompliant
  }

  @Deprecated
  class A {
    Object a = new DeprecatedConstructor();
  }

  class B {
    @Deprecated
    void foo() {
      Object a = new DeprecatedConstructor();
      string.getBytes(1, 1, new byte[3], 7);
     }
  }

}
