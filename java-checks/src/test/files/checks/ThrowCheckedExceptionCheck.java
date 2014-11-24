import java.io.IOException;

class A {
  void method() {
    throw new MyRuntimeException(); //Compliant runtime exception
    throw new IllegalStateException(); //Compliant runtime exception
    throw new MyCheckedException(); //Non-Compliant
    throw new IOException(); //Non-Compliant
  }
}

class MyRuntimeException extends RuntimeException {}
class MyCheckedException extends Exception {}