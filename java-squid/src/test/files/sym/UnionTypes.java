public class UnionTypes {
  public static void foo() throws Exception {
    try {
      bar();
    } catch (A | B e) {
      throw unwrapException(e);
    }
  }

  private static void bar() throws A, B {
    throw new A();
  }

  private static Exception unwrapException(Exception ex) {
    return ex;
  }

  private static A unwrapException(A a) {
    return a;
  }

  private static B unwrapException(B b) {
    return b;
  }
  
  private static class A extends Exception {}
  
  private static class B extends Exception {}
}