abstract class A {
  private void nullCheck(Object o) throws MyException {
    if (o == null) {
      throw new MyException();
    }
  }

  void test(Object o) {
    try {
      nullCheck(o);
    } catch (MyException e) {
      if (o == null) {} // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    }
    o.toString(); // Noncompliant {{A "NullPointerException" could be thrown; "o" is nullable here}}
  }

  void covered() {
    Object o = null;
    try {
      doSomething();
    } catch(MyException e){
      o = new Object();
    }
    if (o == null) {} // Compliant
  }

  private void doSomething() throws MyException {
    exceptionalMethod();
  }

  abstract Object exceptionalMethod() throws MyException;
}

class MyException extends Exception {}
