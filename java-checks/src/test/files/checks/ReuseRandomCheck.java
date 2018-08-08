import java.util.Random;

public class A {

  Random f = new Random(); // Compliant for field

  A() {
    Random v = new Random(); // Compliant in constructor
  }

  void func(long seed, Random p) {
    Random a = new Random(); // Noncompliant [[sc=20;ec=26]] {{Save and re-use this "Random".}}
    Random b = new Random(seed); // Compliant for Random(long seed)
    Object c = new Object();

    p = new Random(); // Noncompliant
  }

  public static void main(String[] args) {
    Random v = new Random(); // Compliant in "main()"
  }

  public class B {
    /*not static*/ void main() {
      Random v = new Random(); // Noncompliant
    }
  }

}
