import java.io.IOException;

class A {
  void method() {
    throw new MyRuntimeException(); //Compliant runtime exception
    throw new IllegalStateException(); //Compliant runtime exception
    throw new MyCheckedException(); // Noncompliant [[sc=11;ec=35]] {{Remove the usage of the checked exception 'MyCheckedException'.}}
    throw new IOException(); // Noncompliant {{Remove the usage of the checked exception 'IOException'.}}
  }
}

class MyRuntimeException extends RuntimeException {}
class MyCheckedException extends Exception {}
