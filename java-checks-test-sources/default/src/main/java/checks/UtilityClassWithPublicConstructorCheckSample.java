package checks;

import java.io.Serializable;
import javax.annotation.CheckForNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

class UtilityClassWithPublicConstructorCheckSample {

  @CheckForNull
  class Coverage { // Noncompliant
    public static void foo() {
    }
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  class LombokClassNoArgsPrivate { // Compliant, a private constructor will be generated
    public static void foo() {
    }
  }

  @NoArgsConstructor(access = AccessLevel.PUBLIC)
  class LombokClassNoArgsPublic { // Noncompliant
    public static void foo() {
    }
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  class LombokClassNoArgsNone { // Compliant
    public static void foo() {
    }
  }

  @lombok.NoArgsConstructor(access = AccessLevel.PUBLIC)
  class LombokClassFullyQualifiedNoArgsPublic { // Noncompliant
    public static void foo() {
    }
  }

  @NoArgsConstructor(force = true)
  class LombokClassNoArgs { // Noncompliant
    public static void foo() {
    }
  }

  @AllArgsConstructor(access = PRIVATE)
  class LombokClassAllArgsPrivate { // Compliant, a private constructor will be generated
    public static void foo() {
    }
  }

  @lombok.AllArgsConstructor(access = PRIVATE)
  class LombokClassFullyQualifiedAllArgsPrivate { // Compliant, a private constructor will be generated
    public static void foo() {
    }
  }

  @RequiredArgsConstructor(access = PRIVATE)
  class LombokClassRequiresPrivate { // Compliant, a private constructor will be generated
    public static void foo() {
    }
  }

  class Foo1 {
  }

  class Foo2 {
    public void foo() {
    }
  }

  class Foo3 { // Noncompliant {{Add a private constructor to hide the implicit public one.}}
//      ^^^^
    public static void foo() {
    }
  }

  class Foo4 {
    public static void foo() {
    }

    public void bar() {
    }
  }

  class Foo5 {
    public Foo5() { // Noncompliant {{Hide this public constructor.}}
    }

    public static void foo() {
    }
  }

  class Foo6 {
    private Foo6() {
    }

    public static void foo() {
    }

    int foo;

    static int bar;
  }

  class Foo7 {

    public <T> Foo7(T foo) { // Noncompliant
    }

    public static <T> void foo(T foo) {
    }

  }

  class Foo8 extends Bar {

    public static void f() {
    }

  }

  class Foo9 {

    public int foo;

    public static void foo() {
    }

  }

  class Foo10 { // Noncompliant

    public static int foo;

    ;

  }

  class Foo11 {

    protected Foo11() {
    }

    public static int a;

  }

  class Foo12 { // Noncompliant
    static class plop {
      int a;
    }
  }

  class Foo13 {

    private Foo13() {
    }

    ;
  }

  class Foo14 { // Noncompliant {{Add a private constructor to hide the implicit public one.}}
//      ^^^^^
    static {
    }
  }

  class Foo15 {
    public Object o = new Object() {
      public static void foo() {
      }
    };
  }

  class Foo16 implements Serializable { // Compliant
    private static final long serialVersionUID = 1L;
  }

  class Foo17 {
    public Foo17() {
      // do something
    }
  }

  class Main { // Compliant - contains main method
    public static void main(String[] args) throws Exception {
      System.out.println("Hello world!");
    }
  }

  class NotMain { // Noncompliant
    static void main(String[] args) throws Exception {
      System.out.println("Hello world!");
    }

    static void main2(String[] args) {
      System.out.println("Hello world!");
    }
  }

  public class MySingleton {
    private void MySingleton2() {
      // use getInstance()
    }

    private static class InitializationOnDemandHolderMySingleton { // compliant inner class is private, adding a private constructor won't change anything
      static final checks.MySingleton INSTANCE = new checks.MySingleton();
    }
    static class InitializationOnDemandHolderMySingleton2 { // Noncompliant
      static final checks.MySingleton INSTANCE = new checks.MySingleton();
    }
    private class InitializationOnDemandHolderMySingleton3 {
      static final checks.MySingleton INSTANCE = new checks.MySingleton();
    }

    public static checks.MySingleton getInstance() {
      return InitializationOnDemandHolderMySingleton.INSTANCE;
    }
  }

  static class CustomLombokLikeAnnotation {
    // Custom Lombok-like annotation.
    @interface NoArgsConstructor {
      AccessLevel access() default AccessLevel.PUBLIC;
    }

    // This one is tricky - the annotation is not the real one, so it is noncompliant.
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class CustomPrivate { // Noncompliant
      public static void foo() {
      }
    }

    @NoArgsConstructor(access = AccessLevel.PUBLIC)
    class CustomPublic { // Noncompliant
      public static void foo() {
      }
    }
  }

  // fix@qf1 {{Add an empty private constructor as the first member of the class.}}
  // edit@qf1 [[sl=+0;el=+0;sc=29;ec=29]] {{\n    private StringUtils() {\n      /* This utility class should not be instantiated */\n    }\n}}
  // fix@qf2 {{Add an empty private constructor as the last member of the class.}}
  // edit@qf2 [[sl=+5;el=+5;sc=6;ec=6]] {{\n    private StringUtils() {\n      /* This utility class should not be instantiated */\n    }\n}}
  // fix@qf3 {{Add an empty private constructor before the first method in the class.}}
  // edit@qf3 [[sl=+1;el=+1;sc=49;ec=49]] {{\n    private StringUtils() {\n      /* This utility class should not be instantiated */\n    }\n}}
  public class StringUtils { // Noncompliant [[sc=16;ec=27;quickfixes=qf1,qf2,qf3]]
    public static String HELLO = "Hello world!";

    public static String concatenate(String s1, String s2) {
      return s1 + s2;
    }
  }

  // fix@qf4 {{Add an empty private constructor as the first member of the class.}}
  // edit@qf4 [[sl=+0;el=+0;sc=43;ec=43]] {{\n    private StringUtilsWithBadOffsets() {\n      /* This utility class should not be instantiated */\n    }\n}}
  // fix@qf5 {{Add an empty private constructor as the last member of the class.}}
  // edit@qf5 [[sl=+5;el=+5;sc=6;ec=6]] {{\n    private StringUtilsWithBadOffsets() {\n      /* This utility class should not be instantiated */\n    }\n}}
  // fix@qf6 {{Add an empty private constructor before the first method in the class.}}
  // edit@qf6 [[sl=+1;el=+1;sc=45;ec=45]] {{\n    private StringUtilsWithBadOffsets() {\n      /* This utility class should not be instantiated */\n    }\n}}
  public class StringUtilsWithBadOffsets { // Noncompliant [[sc=16;ec=41;quickfixes=qf4,qf5,qf6]]
public static String HELLO = "Hello world!";

 public static String concatenate(String s1, String s2) {
      return s1 + s2;
    }
  }
}
