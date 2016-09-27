class Foo {
  public void foo() {
    bar(s -> write1(get(s)));
    bar(s -> write2(get(s)));
  }

  private void bar(java.util.function.Consumer<String> consumer) {
    // ...
  }

  private <D, L extends MyList<D>> void write1(L list) {
    // ...
  }

  private <D, L extends MyList> void write2(L list) {
    // ...
  }

  private <T> MyList<T> get(T t) {
    return new MyList<>();
  }

  private static class MyList<V> { }

}
class A {
  public static <Z extends Enum<Z>> java.util.EnumSet<Z> noneOf(Class<Z> elementType) {
    return null;
  }

  void popopo() {
    noneOf(MyENUM.class);
  }
  enum MyENUM {
    VALUE1,VALUE2;
  }
}
