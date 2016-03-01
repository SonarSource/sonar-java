public class UnionTypes {
  public static void foo() throws Throwable {
    try {
      bar();
    } catch (A | C e0) {
      throw unwrapException(e0);
    } catch (D | E | F e1) {
      throw unwrapException(e1);
    } catch (G | MyUnknownException e2) {
      throw unwrapException(e2);
    }
  }

  private static void bar() throws A, B, C, D, E, F, G, MyUnknownException  {
    throw new A();
  }

  private static Exception unwrapException(Exception e) {
    return e;
  }

  private static B unwrapException(B b) {
    return b;
  }
  
  private static Throwable unwrapException(Throwable t) {
    return t;
  }

  private static class A extends Exception {}

  private static class B extends Exception {}

  private static class C extends B {}

  private static class D extends B {}

  private static class E extends B {}

  private static class F extends B {}
  
  private static class G extends Throwable {}
}
