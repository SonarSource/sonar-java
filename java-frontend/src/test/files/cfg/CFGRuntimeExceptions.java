package org.foo;

abstract class A {

  void runtimeException() {
    try {
      doSomething();
    } catch (RuntimeException re) {
      doSomethingElse();
    } catch (MyRuntimeException mre) {
      doSomethingElse();
    } catch (Error er) {
      doSomethingElse();
    } catch (MyError mer) {
      doSomethingElse();
    } catch (Throwable t) {
      doSomethingElse();
    } catch (MyThrowable mt) {
      doSomethingElse();
    } catch (Exception ex) {
      doSomethingElse();
    } catch (MyException mex) {
      doNothing();
    }
  }

  abstract void doSomething();
  abstract void doSomethingElse();
  abstract void doNothing();
}

class MyException extends Exception {}
class MyError extends Error {}
class MyThrowable extends Throwable {}
class MyRuntimeException extends RuntimeException {}
