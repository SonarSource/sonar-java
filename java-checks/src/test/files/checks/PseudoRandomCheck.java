import java.util.Random;

class A {
  void fun() {
    Random random = new Random(); // Noncompliant [[sc=25;ec=31]] {{Make sure that using this pseudorandom number generator is safe here.}}
    byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    double j = Math.random(); // Noncompliant [[sc=21;ec=27]]
  }
}
