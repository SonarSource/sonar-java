/**
 * some documentation
 */
public class UndocumentedApi { // Compliant - documented
  public String p;
  private String key; // Compliant - private

  public UndocumentedApi() { // Compliant - empty constructor
  }

  public UndocumentedApi(String key) {
    this.key = key;
  }

  public void run() {
  }

  public interface InnerUndocumentedInterface {
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

public enum FooEnum {
}

public interface Ainterface {
}

public @interface FooAnnotation {
}

public class AClass {

  public int a;

  public A() {
    System.out.println();
  }

}

/**
 * This is a Javadoc comment
 */
public class MyClass<T> implements Runnable {

 private int status;                            // Compliant - not public

 /**
   * This is a Javadoc comment
   */
 public String message;                         // Compliant - well documented

 public MyClass() {                            
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
 public int doSomething(int value) {
   return value;
 }

 /** plop
  *  */
 public int doSomething() {
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
  int foo();

  /**
   * @return
   */
  int foo(); // Compliant

  /** plop.
   */
  void foo(int a);
}

/**
 *
 */
class FooClass {
  /** constructor.
   */
  public FooClass(int a) {
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
  /** foo.
   */
  public int foo(int a, int b, int c) {
    return 0;
  }


  public int foo(int a, int b, int c) {
    return 0;
  }

  /**
    * @param <T> foo
    */
  public <T> void foo() { // Compliant - does not return anything
  }

  public <T> void foo() {
  }

  /**
   * @param <T> foo
   */
  public <T> int foo() {
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
public interface bar {
  /**
  * @param <A>  the annotation type
  * @param annotationType  the <tt>Class</tt> object corresponding to
  *          the annotation type
  * @return the annotation of this declaration having the specified type
  *
  * @see #getAnnotationMirrors()
  */
  <A extends Annotation> A getAnnotation(Class<A> annotationType);
  static class A{} 
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
  static final class DEFAULT {}
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

  public void foo
  (
  )
  {
  }

  /** Foo.
   */
  public interface Foo {

    public void foo();

  }

  @Target({METHOD})
  @Retention(RUNTIME)
  public @interface Transient {
      boolean value() default true;
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
