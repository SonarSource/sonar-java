package foo.bar;

abstract class A {
  static void method(boolean b, A param) {
    if (param == null) {
      return;
    }
    if (b) {
      // do something
    }
    param.plantFlowers(b);
  }

  abstract void plantFlowers(boolean b);
}
