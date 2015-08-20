/**
 * some documentation
 */
public class UndocumentedApi { // Compliant - documented
  public String p1; // Noncompliant

  /***/
  public String p2; // Noncompliant {{Document this description.}}

  /** */
  public String p3; // Noncompliant {{Document this description.}}

  /**
   */
  public String p4; // Noncompliant {{Document this description.}}

  /**
   *
{@link}**/
  public String p5; // Noncompliant {{Document this description.}}

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
  *  Do something
   * @param value ...
   */
 public void doSomething(int value) {           // Compliant
 }

 /**
  * @param value ...
  */
public void doSomething(int value) {           // Noncompliant {{Document this description.}}
}


 /**
  * Do something
  * @return foo
  */
 public int doSomething(int value) {            // Noncompliant {{Document the parameter(s): value}}
   return value;
 }

 /**
  * Do someting
  */
public int doSomething() {                     // Noncompliant {{Document this method return value.}}
   return value;
 }

/**
 *
 *
 */
public int doSomething2() {                     // Noncompliant 2
  return value;
}

}

/**
 */
interface FooInterface {  // Compliant - non public
  /**
   * Do something
   */
  void foo(); // Compliant

  /**
   * @author foo
   */
  void foo(); // Noncompliant {{Document this description.}}

  /**
   * Do something
   */
  int foo(); // Noncompliant {{Document this method return value.}}

  /**
   * Do something
   * @return
   */
  int foo(); // Compliant

  /**
   * @return
   */
  int foo(); // Noncompliant {{Document this description.}}

  /**
   * Do something
   */
  void foo(int a); // Noncompliant {{Document the parameter(s): a}}

  /**
   * @param a a
   */
  void foo(int a); // Noncompliant {{Document this description.}}
}

/**
 * @see String
 */
class FooClass { // Compliant - non public
  /**
   * Do something
   */
  public FooClass(int a) { // Noncompliant {{Document the parameter(s): a}}
    System.out.println(a);
  }

  /**
   * Foo bar
   * @param a
   */
  public FooClass(int a) { // Compliant
    System.out.println(a);
  }
}

private class FooPrivate { // Compliant - non public
}

class FooPackage { // Compliant - non public
}

/**
 * {@link String}
 */
public class Foo { // Noncompliant {{Document this description.}}
  /**
   * Do something
   */
  public int foo(int a, int b, int c) { // Noncompliant 2
    return 0;
  }

  /**
   * {@
   */
  public int foo(int a, int b, int c) { // Noncompliant 3
    return 0;
  }

  /**
   * Do something
   * @param a a
   * @param b b
   * @param c c
   */
  public int foo(int a, int b, int c) { // Noncompliant {{Document this method return value.}}
    return 0;
  }

  /**
   * Do something
   * @return result
   */
  public int foo(int a, int b, int c) { // Noncompliant {{Document the parameter(s): a, b, c}}
    return 0;
  }

  public int foo(int a, int b, int c) { // Noncompliant {{Document this public method.}}
    return 0;
  }

  /**
   * Do something
    * @param <T> foo
    */
  public <T> void foo() { // Compliant - does not return anything
  }

  /**
   * @param <T> foo
   */
 public <T> void foo() { // Noncompliant {{Document this description.}}
 }

  public <T> void foo() { // Noncompliant {{Document this public method.}}
  }

  /**
   * Do something
   * @param <T> foo
   */
  public <T> int foo() { // Noncompliant {{Document this method return value.}}
  }

  /**
   * Do something
   * @param <T> foo
   * @return foo
   */
  public <T> int foo() { // Compliant
  }

  public void getThisThingDone() { //false negative this is interpreted as a getter.
  }
}
/**
 * Bar
 * */
public interface bar {
  /**
   * This is the main documentation
   *
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
  static final class DEFAULT {} // Noncompliant {{Document this description.}}
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
  public int foo(int a, int b, int c) { // Compliant - inherited documentation
    return 0;
  }

  private interface Bar {
    void method();
  }

  public void foo
  ( // Noncompliant
  )
  {
  }

  /**
   */
  public interface Foo { // Noncompliant {{Document this description.}}

    public foo(); // Noncompliant

  }

  @Target({METHOD})
  @Retention(RUNTIME)
  public @interface Transient { // Noncompliant
      boolean value() default true; // Noncompliant
  }
}
class AnonymousInnerClass {
  Comparator<String> doJob(){
    return new Comparator<String>() { // anon-inner-class
      class Hello { // inner-class
        public void doJob() { // false-positive
        }
      }

      public int compare(String o1, String o2) {
        return 0;
      }
    };
  }
}
