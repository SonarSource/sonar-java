import java.util.function.Function;

class A {
  private final Runnable invalidator = this::invalidate;
  private final Function<String, Object> myFunc = A::foo;

  private void invalidate() { }
  private static Object foo(String s) { return null; }
}
