package foo.bar;

import javax.annotation.Nonnull;

abstract class A {
  static void method(boolean b, @Nonnull A param) {
    if (b) {
      // do something
    }
    param.plantFlowers(b);
  }

  abstract void plantFlowers(boolean b);
}
