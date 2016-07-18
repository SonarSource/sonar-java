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

public enum FooEnum { // Noncompliant {{Document this public enum.}}
}

public interface Ainterface { // Noncompliant {{Document this public interface.}}
}

public @interface FooAnnotation { // Noncompliant {{Document this public annotation.}}
}

public class AClass { // Noncompliant {{Document this public class.}}

  public int a; // Noncompliant {{Document this public field.}}

  public A() { // Noncompliant {{Document this public constructor.}}
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
   * @param value ...
   */
 public void doSomething(int value) {           // Compliant
 }

 /**
  * @return foo
   */
 public int doSomething(int value) {            // Noncompliant {{Document the parameter(s): value}}
   return value;
 }

 /** plop
  *  */
 public int doSomething() {                     // Noncompliant {{Document this method return value.}}
   return value;
 }
}

/**
 */
interface FooInterface {
  /**
   * void. */
  void foo(); // Compliant

  /**
   * bar. */
  int foo(); // Noncompliant

  /**
   * @return
   */
  int foo(); // Compliant

  /** plop.
   */
  void foo(int a); // Noncompliant {{Document the parameter(s): a}}
}

/**
 * doc.
 */
public class FooClass {
  /** constructor.
   */
  public FooClass(int a) { // Noncompliant {{Document the parameter(s): a}}
    System.out.println(a);
  }

  /**
   * @param a
   */
  public FooClass(int a) { // Compliant
    System.out.println(a);
  }
}

private class FooPrivate { // Compliant - non pubic
}

class FooPackage { // Compliant - non public
}

/** Documented.
 */
public class Foo { // Compliant
  // Noncompliant@+3
  /** foo.
   */
  public int foo(int a, int b, int c) { // Noncompliant
    return 0;
  }


  public int foo(int a, int b, int c) { // Noncompliant {{Document this public method.}}
    return 0;
  }

  /**
    * @param <T> foo
    */
  public <T> void foo() { // Compliant - does not return anything
  }

  public <T> void foo() { // Noncompliant {{Document this public method.}}
  }

  /**
   * @param <T> foo
   */
  public <T> int foo() { // Noncompliant {{Document this method return value.}}
  }

  /**
   * @param <T> foo
   * @return foo
   */
  public <T> int foo() { // Compliant
  }

  public void getThisThingDone() { //false negative this is interpreted as a getter.
  }
}
/**
 * */
public interface bar { // Noncompliant {{Document this public interface.}}
  /**
  * @param <A>  the annotation type
  * @param annotationType  the <tt>Class</tt> object corresponding to
  *          the annotation type
  * @return the annotation of this declaration having the specified type
  *
  * @see #getAnnotationMirrors()
  */
  <A extends Annotation> A getAnnotation(Class<A> annotationType);
  static class A{} // Noncompliant {{Document this public class.}}
  public int i = 0;

  /**
   * documentMethod.
   */
  default void method(){
    int j = 1;
  }
}

@interface nested{
  /**
   *
   */
  static final class DEFAULT {} // Noncompliant {{Document this public class.}}
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
 * Documented
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

  public void foo // Noncompliant [[sc=15;ec=18]]
  (
  )
  {
  }

  /** Foo.
   */
  public interface Foo {

    public foo(); // Noncompliant

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
