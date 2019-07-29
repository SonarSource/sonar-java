import java.text.ParseException;

public class RedundantThrowsDeclarationCheck {

  public void foo1() {
  }

  public void foo2() throws Throwable {
  }

  public void foo3() throws Error {
  }

  public void foo4() throws MyException {
  }

  public void foo5() throws RuntimeException { // Noncompliant [[sc=29;ec=45]] {{Remove the declaration of thrown exception 'java.lang.RuntimeException' which is a runtime exception.}}
  }

  public void foo6() throws IllegalArgumentException { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.IllegalArgumentException' which is a runtime exception.}}
  }

  public void foo7() throws MyRuntimeException { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck$MyRuntimeException' which is a runtime exception.}}
  }

  public void foo8() throws MyException, Exception { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck$MyException' which is a subclass of 'java.lang.Exception'.}}
  }

  public void foo9() throws Error, Throwable { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.Error' which is a subclass of 'java.lang.Throwable'.}}
  }

  public void foo11() throws MyException, MyException { // Noncompliant {{Remove the redundant 'RedundantThrowsDeclarationCheck$MyException' thrown exception declaration(s).}}
  }

  public void foo12() throws MyException, MyException, Throwable { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck$MyException' which is a subclass of 'java.lang.Throwable'.}}
  }

  public void foo13() throws MyRuntimeException, MyRuntimeException { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck$MyRuntimeException' which is a runtime exception.}}
  }

  public void foo14() throws MyRuntimeException, Throwable { // Noncompliant [[sc=30;ec=48]] {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck$MyRuntimeException' which is a subclass of 'java.lang.Throwable'.}}
  }

  public void foo15() throws Exception, Error {
  }

  public class MyException extends Exception {
  }

  public class MyRuntimeException extends RuntimeException {
  }

  static interface MyInterface<T> {
    public T plop() throws IllegalStateException; // Noncompliant {{Remove the declaration of thrown exception 'java.lang.IllegalStateException' which is a runtime exception.}}
  }

  static class MyClass implements MyInterface<String> {
    public String plop() {
      return "";
    }
  }
}

abstract class MySuperClass {
  abstract void foo() throws MyException;
}

abstract class ThrownCheckedExceptions extends MySuperClass {
  public ThrownCheckedExceptions(String s) throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from constructor's body.}}
    bar();
  }

  @Override
  void foo() throws MyException { //Compliant - Override
    bar();
  }

  void foo0() throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from method's body.}}
    bar();
  }

  void foo1() throws MyException { // Compliant - unknown method is called
    unknownMethod();
    bar();
  }

  void foo2() throws MyException { // Compliant - unknown exception is thrown by qix
    qix();
  }

  void foo3() throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from method's body.}}
    new MyClass() {
      void foo() {
        throw new MyException();
      }
    };
  }

  void foo4() throws MyException { // Noncompliant
    gul(o -> { throw new RuntimeException(); });
  }

  void foo5() throws MyException { // Compliant
    bar();
    mok();
  }

  void foo6() { // Compliant
    puf();
    kal();
  }

  void foo7() throws MyException { // Compliant - False-Negative - studying control flow is not done
    try {
      mok();
    } catch(MyException e) {
      // do nothing
    }
  }

  void foo8(Throwable t) throws java.io.IOException { // Compliant
    throwIfException(t, java.io.IOException.class);
  }

  <X> void foo9(int i) throws MyException {
    new MyOtherClass<X>(i);
  }

  protected Object readResolve() throws java.io.ObjectStreamException { // Compliant - Serializable contract
    // do nothing
  }

  int foo9() throws MyException { // Compliant - designed for extension
    return 0;
  }

  public int foo10() throws MyException { // Compliant - designed for extension
    throw new UnsupportedOperationException();
  }

  private int foo11() throws MyException { // Noncompliant - private
    return 0;
  }

  Object foo12() throws MyException { // Noncompliant - only target litteral
    return new Object();
  }

  void foo13() throws MyException { // Compliant - designed for extension
    return;
  }

  void foo14(java.io.File file) throws java.io.IOException { // Compliant - Closeable.close() throws IOException
    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
      // do something
    }
  }

  void foo15(java.io.File file) throws Exception { // Compliant - AutoCloseable.close() throws Exception
    try (AutoCloseable ac = getAutoCloseableWithoutExceptionPlease(file)) {
      // do something
    }
  }

  void foo15_java9(java.io.File file) throws Exception { // Compliant - AutoCloseable.close() throws Exception
    final AutoCloseable ac = getAutoCloseableWithoutExceptionPlease(file);
    try (ac) {
      // do something
    }

  }

  void foo16(java.io.File file) throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from method's body.}}
    try (MyAutoCloseable mac = getAutoCloseableWithoutExceptionPlease(file)) {
      // do something
    }
  }

  void foo17(java.io.File file) throws java.io.IOException { // Compliant MyCloseable inherits from Closeable
    try (MyCloseable mac = getMyCloseable(file)) {
      // do something
    }
  }

  void foo18(java.io.File file) throws ParseException { // Noncompliant {{Remove the declaration of thrown exception 'java.text.ParseException', as it cannot be thrown from method's body.}}
    try (MyCloseable mac = getMyCloseable(file)) {
      // do something
    }
  }

  void foo19(java.io.File file) throws ParseException { // Compliant if we can't resolve types
    try (UnknownClass mac = getUnknownClass(file)) {
      // do something
    }
  }

  void foo20(java.io.File file) throws ParseException { // Compliant - AutoCloseableParserInterface.close() throws ParseException
    try (AutoCloseableParserInterface mac = getAutoCloseableInterfaceWithParseException(file)) {
      // do something
    }
  }

  void foo21(java.io.File file) throws ParseException { // Compliant - AutoCloseableParserClass.close() throws ParseException
    try (AutoCloseableParserClass mac = getAutoCloseableClassWithParseException(file)) {
      // do something
    }
  }

  abstract void bar();
  abstract void qix() throws UnknownException;
  abstract void gul(java.util.function.Function<Object, String> s);
  abstract void mok() throws MyException;
  abstract void puf() throws MyError;
  abstract void kal() throws MyRuntimeException; // Noncompliant
  abstract <X extends Exception> void throwIfException(Throwable t, Class<X> declaredType) throws X;
  static class MyClass { }
  static class MyOtherClass<X> {
    public MyOtherClass(int i) throws MyException { }
  }

  abstract MyAutoCloseable getAutoCloseableWithoutExceptionPlease(java.io.File file);
  abstract MyCloseable getMyCloseable(java.io.File file);
  abstract AutoCloseableParserInterface getAutoCloseableInterfaceWithParseException(java.io.File file);
  abstract AutoCloseableParserClass getAutoCloseableClassWithParseException(java.io.File file);
  interface MyAutoCloseable extends AutoCloseable {
    @Override
    void close(); // override which does not throw 'java.lang.Exception'
  }
  interface MyCloseable extends java.io.Closeable {
  }
  interface AutoCloseableParserInterface extends AutoCloseable {
    @Override
    void close() throws ParseException;
  }
  static class AutoCloseableParserClass implements AutoCloseable {
    int close = 2;
    public void close(int i) {
    }
    @Override
    public void close() throws ParseException {
      throw new ParseException("", 0);
    }
  }

  interface MyOtherInterface {
    Object foo(Object o) throws MyException; // Compliant

    default Object bar(Object o) throws MyException { // Compliant - default method are also defining a contract
      return o;
    }

    default Object qix(Object o) throws MyException, MyException { // Noncompliant
      return o;
    }
  }
}

class MyException extends Exception {}
class MyError extends Error {}
class MyRuntimeException extends RuntimeException {}
class MyException2 extends Exception {}

abstract class NonThrownExceptionClass {
  abstract void bar();

  final class FinalClass {

    /**
     * @throws MyException proper javadoc description
     */
    void nonOverrideableMethod() throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from method's body.}}
      bar();
    }
  }

  class NormalClass {
    /**
     * @exception MyException proper javadoc description
     */
    private void nonOverrideableMethod1() throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from method's body.}}
      bar();
    }

    /**
     * @throws MyException proper javadoc description
     */
    static void nonOverrideableMethod2() throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from method's body.}}
      bar();
    }

    /**
     * @throws MyException proper javadoc description
     */
    final void nonOverrideableMethod3() throws MyException { // Noncompliant {{Remove the declaration of thrown exception 'MyException', as it cannot be thrown from method's body.}}
      bar();
    }

    /**
     * @throws MyException proper javadoc description
     */
    void overrideableMethod1() throws MyException { // Compliant : can be overridden and has javadoc associated
      bar();
    }

    /**
     * @throws MyException proper javadoc description
     */
    protected void overrideableMethod2() throws MyException { // Compliant : can be overridden and has javadoc associated
      bar();
    }

    /**
     * @exception MyException proper javadoc description
     */
    public void overrideableMethod3() throws MyException { // Compliant : can be overridden and has javadoc associated
      bar();
    }

    /**
     * Overridable method but javadoc for exception is missing proper description
     * @exception MyException
     */
    public void overrideableMethod4() throws MyException { // Noncompliant
      bar();
    }

    /**
     * @exception MyException proper javadoc description
     * @throws  MyException2 proper javadoc description
     */
    public void missingJavadocForException() throws MyException, MyException2, java.io.IOException { // Noncompliant {{Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from method's body.}}
      bar();
    }
  }
}

abstract class Test {
  abstract void foo() throws Unknown, Unknown;
}

class Parent {
  public Parent() throws IllegalAccessException { // Compliant
    throw new IllegalAccessException();
  }

  public Parent(String a) {

  }

  public Parent(String a, String b) throws IllegalAccessException { // Compliant
    foo();
  }

  private void foo() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
}

class Child extends Parent {

  public Child(Integer a) throws IllegalAccessException { // Compliant, implicit call can throw IllegalAccessException
    // implicit call to parent constructor
    System.out.println("a:" + a);
  }

  public Child(Long a) throws IllegalAccessException { // Compliant, equivalent to "Child(Integer a)"
    super();
    System.out.println("a:" + a);
  }

  public Child(Float a) throws IllegalAccessException { // Noncompliant
    this(4.2, 4.2);
    System.out.println("a:" + a);
  }

  public Child(Double a) throws IllegalAccessException { // Noncompliant
    super("a:" + a);
    System.out.println("a:" + a);
  }

  public Child(Integer a, Integer b) throws IllegalAccessException { // Compliant, call to Parent that calls foo, that thows IllegalAccessException
    super("a:" + a, " b:" + a);
    System.out.println("a:" + a);
  }

  public Child(Double d1, Double d2) {
    super("a");
  }
}

class A {
  class Parent1 {
    public Parent1() throws IllegalAccessException { // Compliant
      throw new IllegalAccessException();
    }
    public Parent1(String a) { // Compliant
    }
  }

  class Child1 extends Parent1 {
    public Child1(Long a) throws IllegalAccessException { // FN, the symbol of the call to super is not properly resolved
      A.this.super("a:" + a);
    }
  }
}

class B {
  class Parent2{
    public Parent2() throws IllegalAccessException { // Compliant
      throw new IllegalAccessException();
    }
    public Parent2(String a) { // Compliant
    }
  }

  class Child2 extends Parent2 {
    public Child2(Long a) throws IllegalAccessException { // Compliant
      //implicit call to Parent2. Parent2() will have an implicit parameter A
      System.out.println("a:" + a);
    }
  }
}

class C extends UnknownParent {
  public C() throws IllegalAccessException {

  }
}

enum MyEnum {
  AAA(7), BBB(2);

  MyEnum(int i) throws IllegalAccessException {
  }
}

