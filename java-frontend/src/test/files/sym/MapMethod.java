
class Foo {

  public void foo(java.util.List<String> list) {
    list.stream()
      .filter(s -> (s.length() > 42))
      .map(s -> test1(s))
      .map(b -> test2(b));

    list.stream()
      .map(String::length)
      .filter(x -> test3(x) > 0);
  }

  private boolean test1(String s) {
    return false;
  }

  private int test2(boolean b) {
    return 0;
  }

  private int test3(int x) {
    return x - 1;
  }

}