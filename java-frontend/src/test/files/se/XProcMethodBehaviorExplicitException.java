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
    o.toString(); // Noncompliant {{NullPointerException might be thrown as 'o' is nullable here}}
  }

  void notCovered() {
    Object o = null;
    try {
      doSomething();
    } catch(MyException e){
      o = new Object();
    }
    if (o == null) {} // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
                      // FP: no yield from implicit MyException occuring in doSomething()
  }

  private void doSomething() throws MyException {
    exceptionalMethod(); // implicit exception not handled
  }

  abstract Object exceptionalMethod() throws MyException;
}

class MyException extends Exception {}
