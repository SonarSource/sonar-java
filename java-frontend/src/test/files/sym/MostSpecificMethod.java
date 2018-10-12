public class A implements I {
  public A() {
    foo(String.class);
  }

  @Override
  public final <T> T foo(Class<T> clazz) {
    return null;
  }
}

interface I {
  public <T> T foo(Class<T> clazz);
}
