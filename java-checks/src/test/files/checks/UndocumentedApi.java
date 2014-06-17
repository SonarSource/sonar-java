/**
 * some documentation
 */
public class UndocumentedApi { // Compliant - documented
  public String p; // Non-Compliant
  private String key; // Compliant - private

  public UndocumentedApi() { // Compliant - empty constructor
  }

  public UndocumentedApi(String key) { // Non-Compliant
    this.key = key;
  }

  public void run() { // Non-Compliant
  }

  public interface InnerUndocumentedInterface { // Non-Compliant
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

public enum Foo { // Non-Compliant
}

public interface A { // Non-Compliant
}

public @interface Foo { // Non-Compliant
}

public class A { // Non-Compliant

  public int a; // Non-Compliant

  public A() { // Non-Compliant
    System.out.println();
  }

}

/**
 * This is a Javadoc comment
 */
public class MyClass<T> implements Runnable {    // Non-Compliant - missing '@param <T>'

 private int status;                            // Compliant - not public

 /**
   * This is a Javadoc comment
   */
 public String message;                         // Compliant - well documented

 public MyClass() {                             // Non-Compliant - missing documentation
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
 public int doSomething(int value) {            // Non-Compliant - missing '@param value'
   return value;
 }

 /**
   */
 public int doSomething() {                     // Non-Compliant - missing '@return'
   return value;
 }
}

/**
 */
interface Foo {
  /**
   */
  void foo(); // Compliant

  /**
   */
  int foo(); // Non-Compliant

  /**
   * @return
   */
  int foo(); // Compliant

  /**
   */
  void foo(int a); // Non-Compliant
}

/**
 *
 */
class Foo {
  /**
   */
  public Foo(int a) { // Non-Compliant
    System.out.println(a);
  }

  /**
   * @param a
   */
  public Foo(int a) { // Compliant
    System.out.println(a);
  }
}

private class Foo { // Compliant - non pubic
}

class Foo { // Compliant - non public
}

/**
 */
public class Foo { // Compliant
  /**
   */
  public int foo(int a, int b, int c) { // Non-Compliant - single issue for parameters, + one for return value
    return 0;
  }


  public int foo(int a, int b, int c) { // Non-Compliant - single issue for complete method
    return 0;
  }

  /**
    * @param <T> foo
    */
  public <T> void foo() { // Compliant - does not return anything
  }

  public <T> void foo() { // Noncompliant - must document <T>
  }

  /**
   * @param <T> foo
   */
  public <T> int foo() { // Noncompliant - must document return value
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
