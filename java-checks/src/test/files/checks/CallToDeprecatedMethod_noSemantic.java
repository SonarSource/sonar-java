package java.lang;
class CallToDeprecatedMethod {

  public CallToDeprecatedMethod() {
    String string = new String("my string");
    string.getBytes(1, 1, new byte[3], 7); // Noncompliant [[sc=12;ec=20]] {{Remove this use of "getBytes"; it is deprecated.}}
    new DeprecatedConstructor(); // Noncompliant [[sc=9;ec=30]] {{Remove this use of "DeprecatedConstructor"; it is deprecated.}}
    new MyDeprecatedClass(); // Noncompliant
    old++; // Noncompliant
    MyDeprecatedClass.a++; // Noncompliant
  }

  void useDeprecatedEum(Object o) {
    useDeprecatedEum(DeprecatedEnum // Noncompliant
      .A); // Noncompliant
    useDeprecatedEum(DeprecatedEnum // Noncompliant
      .B); // Noncompliant
    useDeprecatedEum(PartiallyDeprecatedEnum.C);
    useDeprecatedEum(PartiallyDeprecatedEnum.D); // Noncompliant
  }

  @Deprecated
  int old;

  @Deprecated
  private static class MyDeprecatedClass {
    static int a;
  }

  public static class ExtendsDeprecatedClass extends MyDeprecatedClass { // Noncompliant [[sc=54;ec=71]] {{Remove this use of "MyDeprecatedClass"; it is deprecated.}}
  }

  public static abstract class ClassWithDeprecatedMethods {
    @Deprecated
    public void deprecatedMethod1() {
    }
    @Deprecated
    public void deprecatedMethod2() {
    }
    @Deprecated
    public abstract void deprecatedMethod3();
  }

  public interface InterfaceWithDeprecatedMethods {
    @Deprecated
    public void deprecatedMethod4();
  }

  public static class ClassOverridingDeprecatedMethods extends ClassWithDeprecatedMethods implements InterfaceWithDeprecatedMethods {
    public void deprecatedMethod1() { // Noncompliant [[sc=17;ec=34]] {{Don't override a deprecated method or explicitly mark it as "@Deprecated".}}
    }
    @Deprecated
    public void deprecatedMethod2() { // Compliant, explicitely marked as "@Deprecated"
    }
    public void deprecatedMethod3() { // Compliant, override abstract method
    }
    public void deprecatedMethod4() { // Compliant, override interface
    }
  }

  private static class DeprecatedConstructor {
    @Deprecated
    public DeprecatedConstructor() {
      "".getBytes(1, 1, new byte[3], 7);
    }
    String a  = new String(new byte[3], 7); // Noncompliant
  }

  @Deprecated
  class A {
    Object a = new DeprecatedConstructor();
  }

  class B {
    @Deprecated
    void foo() {
      Object a = new DeprecatedConstructor();
      "".getBytes(1, 1, new byte[3], 7);
    }
  }
}

@Deprecated
enum DeprecatedEnum {
  A,
  B;
}

enum PartiallyDeprecatedEnum {
  C,
  @Deprecated D;
}

class MyCustomClass {
  @Deprecated
  MyDeprecatedType myVar;
}

@Deprecated
class MyDeprecatedType {
  // some stuff
}
