package org.foo.api.mypackage;
/**
 * some documentation
 */
public class UndocumentedApi { // Compliant - documented
  public String p; // Noncompliant [[sc=17;ec=18]]
  private String key; // Compliant - private

  public UndocumentedApi() { // Compliant - empty constructor
  }

  public UndocumentedApi(String key) { // Noncompliant
    this.key = key;
  }

  public void run() { // Noncompliant
  }

  public interface InnerUndocumentedInterface { // Noncompliant
  }

  /**
   * no violation, because documented
   */
  public void run2() {
  }

  public void setKey(String key) { // Compliant - setter
    this.key = key;
  }

  public String getKey() { // Compliant - getter
    return key;
  }

  @Override
  public String toString() { // Compliant - method with override annotation
    return key;
  }

  public static final int FOO = 0; // Compliant - static constant
  private static final int BAR = 0; // Compliant - private
  int a = 0; // Compliant

}

public enum FooEnum { // Noncompliant {{Document this public enum by adding an explicit description.}}
}

public interface Ainterface { // Noncompliant {{Document this public interface by adding an explicit description.}}
}

public @interface FooAnnotation { // Noncompliant {{Document this public annotation by adding an explicit description.}}
}

public class AClass { // Noncompliant {{Document this public class by adding an explicit description.}}

  public int a; // Noncompliant {{Document this public field by adding an explicit description.}}

  public A() { // Noncompliant {{Document this public constructor by adding an explicit description.}}
    System.out.println();
  }

}

/**
 * This is a Javadoc comment
 */
public class MyClass<T> implements Runnable {    // Noncompliant {{Document the parameter(s): <T>}}

 private int status;                            // Compliant - not public

 /**
   * This is a Javadoc comment
   */
 public String message;                         // Compliant - well documented

 public MyClass() {                             // Noncompliant
   this.status = 0;
 }

 public void setStatus(int status) {            // Compliant - setter
   this.status = status;
 }

 @Override
 public void run() {                            // Compliant - has @Override annotation
 }

 protected void doSomething() {                 // Compliant - not public
 }

 /**
   * Valid descriptions.
   * @param value Valid descriptions.
   */
 public void doSomething(int value) {           // Compliant
 }

 /**
  * Valid descriptions.
  * @return foo Valid descriptions.
   */
 public int doSomething(int value) {            // Noncompliant {{Document the parameter(s): value}}
   return value;
 }

 /** Valid descriptions.
  *  */
 public int doSomething() {                     // Noncompliant {{Document this method return value.}}
   return value;
 }
}

/**
 */
interface FooInterface {
  /**
   * Valid descriptions. */
  void foo(); // Compliant

  /**
   * Valid descriptions. */
  int foo(); // Noncompliant

  /**
   * Valid descriptions.
   * @return Valid descriptions.
   */
  int foo(); // Compliant

  /** Valid descriptions.
   */
  void foo(int a); // Noncompliant {{Document the parameter(s): a}}
}

/**
 * Valid descriptions.
 */
public class FooClass {
  /** Valid descriptions.
   */
  public FooClass(int a) { // Noncompliant {{Document the parameter(s): a}}
    System.out.println(a);
  }

  /**
   * Valid descriptions.
   * @param a Valid descriptions.
   */
  public FooClass(int a) { // Compliant
    System.out.println(a);
  }
}

private class FooPrivate { // Compliant - non pubic
}

class FooPackage { // Compliant - non public
}

/** Valid descriptions..
 */
public class Foo { // Compliant
  // Noncompliant@+3
  /** Valid descriptions.
   */
  public int foo(int a, int b, int c) { // Noncompliant
    return 0;
  }


  public int foo(int a, int b, int c) { // Noncompliant {{Document this public method by adding an explicit description.}}
    return 0;
  }

  /**
    * Valid descriptions.
    * @param <T> foo
    */
  public <T> void foo() { // Compliant - does not return anything
  }

  public <T> void foo() { // Noncompliant {{Document this public method by adding an explicit description.}}
  }

  /**
   * Valid descriptions.
   * @param <T> foo
   */
  public <T> int foo() { // Noncompliant {{Document this method return value.}}
  }

  /**
   * Valid descriptions.
   * @param <T> foo Valid descriptions.
   * @return Valid descriptions.
   */
  public <T> int foo() { // Compliant
  }

  public void getThisThingDone() { //false negative this is interpreted as a getter.
  }
}
/**
 * */
public interface bar { // Noncompliant {{Document this public interface by adding an explicit description.}}
  /**
  * Valid descriptions.
  * @param <A>  the annotation type
  * @param annotationType  the <tt>Class</tt> object corresponding to
  *          the annotation type
  * @return the annotation of this declaration having the specified type
  *
  * @see #getAnnotationMirrors()
  */
  <A extends Annotation> A getAnnotation(Class<A> annotationType);
  static class A{} // Noncompliant {{Document this public class by adding an explicit description.}}
  public int i = 0;

  /**
   * Valid descriptions.
   */
  default void method(){
    int j = 1;
  }

  void getValue(); // Compliant
}

@interface nested{
  /**
   *
   */
  static final class DEFAULT {} // Noncompliant {{Document this public class by adding an explicit description.}}
  public int i = 0;
}
/**
 * Documented
 */
@Deprecated
public interface deprecatedInterface{
  /**
   * Doc
   */
  @Deprecated
  enum Location {
    CLASS_TREE;
  }

  public static final Object constant = new AnonymousClass(){
    public void undocumentedMethod(){};
  };

}

/**
 * Valid descriptions.
 */
public class MyRunner extends Foo {

  /**
   * {@inheritDoc}
   */
  public int foo(int a, int b, int c) { // Compliant
    return 0;
  }

  private interface Bar {
    void method();
  }

  public void foo // Compliant - Override Foo.foo()
  (
  )
  {
  }

  /** Valid descriptions.
   */
  public interface Foo {

    public void foo(); // Noncompliant

  }

  @Target({METHOD})
  @Retention(RUNTIME)
  public @interface Transient { // Noncompliant [[sc=21;ec=30]]
      boolean value() default true; // Noncompliant
  }
}
class AnonymousInnerClass {
  Comparator<String> doJob(){
    return new Comparator<String>() { // anon-inner-class
      class Hello { // inner-class
        public void doJob() {
        }
      }

      public int compare(String o1, String o2) {
        return 0;
      }
    };
  }
}

class PublicConstructorOfNonPublicClass {
  public PublicConstructorOfNonPublicClass(int a){
  }
}

/**
 * Documented
 */
class A {

  /**
   * Valid descriptions.
   */
  public void doSomething() { }

  /**
   * Valid descriptions.
   */
  public static class B extends A {
    public void doSomething() { } // Compliant - Override
  }

}

@interface MyAnnotation {}

class UsesVisibleForTesting {
  @org.fest.util.VisibleForTesting
  public void doSomething() {}  // Compliant

  @com.google.common.annotations.VisibleForTesting
  public void doSomethingElse() {} // Compliant

  @MyAnnotation
  public void doNothing() {} // Noncompliant
}

@Deprecated
public class DeprecatedAPI { //Compliant
  public void bar() {} // Noncompliant

  @Deprecated
  public void foo() {} // Compliant

  @org.foo.qix.Deprecated
  public void foo() {} // Noncompliant
}
