import java.io.IOException;

class A {
  void method() {
    throw new MyRuntimeException(); //Compliant runtime exception
    throw new IllegalStateException(); //Compliant runtime exception
    throw new MyCheckedException(); // Noncompliant {{Remove the usage of the checked exception 'MyCheckedException'.}}
//        ^^^^^^^^^^^^^^^^^^^^^^^^
    throw new IOException(); // Noncompliant {{Remove the usage of the checked exception 'IOException'.}}
    throw new MyError(); // Compliant not an exception
  }
}

class MyRuntimeException extends RuntimeException {}
class MyCheckedException extends Exception {}
class MyError extends Error {}

interface MyInterface {
  void foo() throws MyCheckedException;
  void bar();
  void qix() throws IOException;
}

class B implements MyInterface {
  static {
    try {
      throw new MyCheckedException(); // Noncompliant {{Remove the usage of the checked exception 'MyCheckedException'.}}
    } catch (Exception e) {
    }
  }

  public void foo() throws MyCheckedException {
    throw new MyCheckedException(); // Compliant
  }

  public void bar() {
    throw new MyCheckedException(); // Noncompliant {{Remove the usage of the checked exception 'MyCheckedException'.}}
  }

  public void qix() throws IOException {
    throw new MyCheckedException(); // Noncompliant {{Remove the usage of the checked exception 'MyCheckedException'.}}
  }
}
