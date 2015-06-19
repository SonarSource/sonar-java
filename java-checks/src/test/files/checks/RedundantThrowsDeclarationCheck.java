package org.sonar.java.checks.targets;

public class RedundantThrowsDeclarationCheck {

  public void foo1() { // Compliant
  }

  public void foo2() throws Throwable { // Compliant
  }

  public void foo3() throws Error { // Compliant
  }

  public void foo4() throws MyException { // Compliant
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
