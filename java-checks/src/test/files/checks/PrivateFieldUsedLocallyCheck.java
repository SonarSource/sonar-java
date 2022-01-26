import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

class Container {

  private static void foo(int p) {
  }

  class Class1 {
    private int privateField1;            // Noncompliant {{Remove the "privateField1" field and declare it as a local variable in the relevant methods.}}
    private static int staticPrivateField; // Noncompliant [[startColumn=24;endLine=+0;endColumn=42]]
    private int privateField2 = 42;
    public int publicField;

    void method1(boolean cond) {
      if (cond) {
        privateField1 = 5;
      } else {
        this.privateField1 = 42;
      }
      foo(privateField1);
      foo(privateField2);
    }

    void method3() {
      publicField = 42;
      foo(publicField);
    }

    void method4() {
      staticPrivateField = 4;
      foo(staticPrivateField);
    }

    abstract void method4();
  }

  // field's values are written conditionally
  class Class2 {

    private int privateField1;

    void method1(boolean cond) {
      if (cond) {
        privateField1 = 5;
      }

      foo(privateField1);
    }
  }

  // not used - OK
  class Class3 {
    private int privateField = 42;
    void method() {}
  }

  // used outside the methods - OK
    class Class4 {
    private int privateField1 = 5;
    private int privateField2 = privateField1 + 1;

    public int publicField = privateField2 + 1;
  }

  // not read - Not OK
  class Class5 {
    private int privateField; // Noncompliant
    private void method() {
      privateField = 5;
    }
  }

  // field's value is read on other instance - ignore
  class Class6 {
    private Class6 that = new Class6();
    private int privateField1;
    private int privateField3;
    private int privateField2; // Noncompliant

    private void method2(int value) {
      this.privateField1 = value;
      foo(that.privateField1);
    }

    private void method3() {
      privateField2 = 3;
      foo(privateField2);
    }

    private void method4(int value) {
      this.privateField3 = value;
      foo(new Class6().privateField3);
    }
  }

  // nested class
  class Class7 {
    private int privateField;

    class NestedClass {
      public void nestedClassMethod() {
        foo(privateField);
      }
    }

    void method1() {
      privateField = 1;
    }
  }

  // local class
  class Class8 {
    private int privateField;

    void method1() {
      class NestedClass {
        public void nestedClassMethod() {
          foo(privateField);
        }
      }
      return new NestedClass();
    }

    void method2() {
      privateField = 5;
    }

  }

  class Class9 {
    private static int privateField; // no issue. used outside method

    static {
      privateField = 42;
    }
  }

  // static field
  class Class10 {
    private static int privateField;

    static void method1() {
      privateField = 1;
    }

    void method2() {
      foo(Class10.privateField);
    }
  }

  // used in switch-case expression
  class Class11 {
    private static final int privateField = 1;

    void method1() {
      switch (1) {
        case privateField:
          foo(1);
          break;
        default:
          foo(2);
          break;
      }
    }
  }

  // lambda FN
  class Class12 {
    int privateField = 3; // FN

    void plop(IntFunction i) {
    }

    void test() {
      privateField = 2;
      plop(i-> privateField + 1);
    }
  }

  // used in sibling class
  class Class13 {
    class A {
      private int privateField;

      void method1() {
        privateField = 1;
        foo(privateField);
      }
    }

    class B {
      void method2() {
        A a = new A();
        foo(a.privateField);
      }
    }
  }

  // used in lambda
  public class Class15 {
    private String privateField = "foo";

    void method() {
      foo(i -> bar(i, privateField));
    }
  }

  class Class16 {

    private int privateField;  // OK

    void method1() {
      privateField = 42;

      method2();

      if (privateField) {
      }

    }

    void method2() {
      privateField = 1;
    }

  }

  // Constructor are also supported
  class Class17 {
    private int privateField; // Noncompliant
    Class17() {
      privateField = 5;
    }
  }
}

class UsageOfLogger {
  private static final Logger LOGGER = LoggerFactory.getLogger(UsageOfLogger.class); // Compliant - constants
  private final int value = 42; // Noncompliant

  private void runRunRun(Runnable runnable) {
    try {
      runnable.run();
    } catch (IOException e) {
      LOGGER.error("boom", e);
      System.out.println(value);
      throw new RuntimeException("bye bye", e);
    }
  }
}

class AnnotatedPrivateFields {
  @javax.inject.Inject
  private Object injectedService; // Compliant

  @Deprecated
  private String deprecated; // Compliant

  public String doRandomStuff(String s) {
    injectedService = s;
    deprecated = injectedService.toString();
    return deprecated + injectedService.toString();
  }
}

class Example {
  @lombok.Getter
  private int fieldWithGetter; // Compliant
  private int value = 42; // Noncompliant
  @AnyAnnotation
  private int annotatedField; // Compliant
  
  private void foo() {
      fieldWithGetter = 1;
      value = 2;
  }

}

enum A {
  VALUE_1(1),
  VALUE_2(2);

  static class Inner {
    private static java.util.HashMap<Integer, A> INDEX = new java.util.HashMap<>();
  }
  java.util.HashMap field = Inner.INDEX;
  A(int indexValue) {
    Inner.INDEX.put(indexValue, this);
  }

  public static A lookup(int indexValue) {
    return Inner.INDEX.get(indexValue);
  }
}

record PrivateFieldUsedLocallyCheckRecord() {
  static String s = OtherClass.f + "";
  static class OtherClass {
    private static int f = 12;
  }
}
