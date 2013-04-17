class Example {

  class Foo {
    public void method(Bar.Base base) {
      System.out.println(Bar.Base.b);
    }
  }

  static class Bar {
    static class Base {
      static int b;
    }
  }

}
