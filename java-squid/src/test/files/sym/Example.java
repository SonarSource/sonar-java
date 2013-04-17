@SuppressWarnings("all")
class Example {

  class Foo {
    public void method(Bar.Base base) {
      System.out.println(Bar.Base.b);
      int i;
      for (i = 0; i < 10; i++) {
      }
    }
  }

  static class Bar {
    static class Base {
      static int b;
    }
  }

}
