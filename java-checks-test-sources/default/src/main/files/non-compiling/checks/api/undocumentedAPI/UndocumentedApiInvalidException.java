package org.foo;

/**
 * This is documented
 */
class A {
  /**
   * This is documented
   * @throws MyException
   */
  public void foo() throws MyException<Object> {} // Does not compile - trigger exception

}

class MyException<T> extends Exception { }
