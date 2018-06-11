public class A implements I {
  public A() {
    foo(String.class); // resolved to I.foo(...)
  }

  @Override
  public final <T> T foo(Class<T> clazz) { // not resolved as being used in constructor
    return null;
  }
}

interface I {
  public <T> T foo(Class<T> clazz);
}
