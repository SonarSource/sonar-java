abstract class ReturnAndFinally {

  private Object foo(Object a) {
    try {
      Object b = bar(a);
      if (b != null) {
        return b;
      }
    } finally {
      System.out.println("foo");
    }
    return null;
  }

  private Object qix(Object a) {
    try {
      Object b = bar(a);
      if (b != null) {
        // 'a' has no constraint, 'b' is null
        return a;
      }
    } finally {
      if (a != null) {
        // 'a' is not null, 'b' is null
        throw new RuntimeException();
      }
    }
    // 'a' is null, 'b' is null
    return null;
  }

  public abstract Object bar(Object o) throws RuntimeException;
  private class MyException extends Exception {}

  private boolean returnInFinally() {
    try {
      foo();
    } catch (MyException e) {
      throw e;
    } finally {
      if(true) return true;
    }
    return false;
  }

  abstract void foo() throws MyException;

  private Exception returningException() {
    try {
      doSomething();
    } catch (Exception e) {
      return e;
    }
    return null;
  }
  abstract void doSomething() throws Exception;
}
