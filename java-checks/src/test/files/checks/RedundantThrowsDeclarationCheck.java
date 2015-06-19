package org.sonar.java.checks.targets;

public class RedundantThrowsDeclarationCheck {

  private RedundantThrowsDeclarationCheck field = RedundantThrowsDeclarationCheck.foo2();

  RedundantThrowsDeclarationCheck() // Noncompliant {{Remove the declaration of thrown exception 'java.lang.IllegalStateException' which is a runtime exception.}}
  throws IllegalStateException {
    throw new Throwable();
  }

  RedundantThrowsDeclarationCheck() // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck.MyException' which is a subclass of 'java.lang.Throwable'.}}
  throws MyException,
  Throwable {
    throw new Throwable();
  }

  public void foo1() { // Compliant
  }

  public void foo2() throws Throwable { // Compliant, method could be overriden
  }

  public void foo3() throws Error { // Compliant, method could be overriden
  }

  public void foo4() throws MyException { // Compliant, method could be overriden
  }

  public void foo5() throws RuntimeException { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.RuntimeException' which is a runtime exception.}}
  }

  public void foo6() throws IllegalArgumentException { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.IllegalArgumentException' which is a runtime exception.}}
  }

  public void foo7() throws MyRuntimeException { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck.MyRuntimeException' which is a runtime exception.}}
  }

  public void foo8() throws MyException, Exception { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck.MyException' which is a subclass of 'java.lang.Exception'.}}
  }

  public void foo9() throws Error, Throwable { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.Error' which is a subclass of 'java.lang.Throwable'.}}
  }

  public void foo10() throws Throwable, Error { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.Error' which is a subclass of 'java.lang.Throwable'.}}
  }

  public void foo11() throws MyException, MyException { // Noncompliant {{Remove the redundant 'RedundantThrowsDeclarationCheck.MyException' thrown exception declaration(s).}}
  }

  public void foo12() throws MyException, Throwable { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck.MyException' which is a subclass of 'java.lang.Throwable'.}}
  }

  public void foo13() throws MyRuntimeException, MyRuntimeException { // Noncompliant 2 {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck.MyRuntimeException' which is a runtime exception.}}
  }

  public void foo14() throws MyRuntimeException, Throwable { // Noncompliant {{Remove the declaration of thrown exception 'RedundantThrowsDeclarationCheck.MyRuntimeException' which is a runtime exception.}}
  }

  public void foo15() throws Exception, Error { // Compliant
  }

  public void foo16() throws UnknownException1, UnknownException2 {
    // unresolved exceptions resolves to the same symbol but should not raise a duplicate exception issue
  }

  public class MyException extends Exception {
  }

  public class MyRuntimeException extends RuntimeException {
  }

  static interface MyInterface<T> {
     public T plop() throws IllegalStateException; // Noncompliant {{Remove the declaration of thrown exception 'java.lang.IllegalStateException' which is a runtime exception.}}

     public T plop() throws Exception, java.io.IOException; // Noncompliant {{Remove the declaration of thrown exception 'java.io.IOException' which is a subclass of 'java.lang.Exception'.}}

     public void throwingMethod() throws Exception; // Compliant, abstract methods are not checked for thrown exceptions from body
  }

  static class MyClass implements MyInterface<String> {
    @Override
    public String plop() { // Compliant
      return "";
    }
  }
}

class NonfinalClass {
  public NonfinalClass() throws Exception { // Compliant, could be called from a derived class
  }

  private privateMethod() throws Exception { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.Exception' which cannot be thrown from the body.}}
  }

  protected protectedMethod() throws Exception { // Compliant, could be overriden
  }

  public publicMethod() throws Exception { // Compliant, could be overriden
  }

  static staticMethod() throws Exception { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.Exception' which cannot be thrown from the body.}}
  }

  public static void multipleBodyExceptions(boolean condition) throws Exception, Error { // Compliant
    if(condition) {
      throw new Exception();
    } else {
      throw new Error();
    }
  }

  public static void indirectExceptions(boolean condition) throws Exception, Error {
    multipleBodyExceptions(condition);
  }

  public static void indirectExceptionsFromConstructor() throws java.io.FileNotFoundException {
    new java.io.FileInputStream("");
  }

}

final class FinalClass {
  public FinalClass() throws Exception { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.Exception' which cannot be thrown from the body.}}
  }

  public method() throws Exception { // Noncompliant {{Remove the declaration of thrown exception 'java.lang.Exception' which cannot be thrown from the body.}}
  }
}

public enum MyEnum {
  ;
  // initialized fields in enums should not trigger an exception
  private RedundantThrowsDeclarationCheck field = RedundantThrowsDeclarationCheck.foo2();
}

public enum MyInterface {
  // initialized fields in interfaces should not trigger an exception (this construct is invalid in java).
  private RedundantThrowsDeclarationCheck field = RedundantThrowsDeclarationCheck.foo2();
}
