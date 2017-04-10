class A {
  public final boolean foo(Integer... values) {
    if (values.length == 1) {
      return foo(values[0]);
    }
    return false;
  }

  private final boolean foo(int val) {
    return 42 == val;
  }
}
class B {
  public final boolean bar(Object... values) {
    if (values.length == 1) {
      return bar((String) values[0]);
    }
    return false;
  }

  private final boolean bar(String val) {
    return val == null;
  }
}
