import java.util.Random;

class A {
  void fun() {
    Random random = new Random(); //NonCompliant
    byte bytes[] = new byte[20];
    random.nextBytes(bytes);
    double j = Math.random(); //NonCompliant
  }
}