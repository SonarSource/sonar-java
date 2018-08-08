import java.util.Random;

public class A {

  Random field = new Random();

  A() {
    Random v = new Random();
  }

  void func(long seed) {
    Object v;
    v = new Random(seed);
    v = new Random(); // Noncompliant [[sc=13;ec=19]] {{Save and re-use this "Random".}}
    v = new Object();

    enum E {
      A, B, C;
      Random field = new Random();
    }
  }

  static void staticFunc(long seed) {
    Object v = new Random();
  }

  interface I {
    Random field = new Random();
  }
}

public @interface J {
  Random field = new Random();
}
