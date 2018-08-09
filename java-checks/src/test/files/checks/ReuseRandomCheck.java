import java.util.Random;

public class A {

  static Random s = new Random(); // Compliant for static field
  Random f = new Random(); // Compliant for field

  A() {
    Random v = new Random(); // Compliant in constructor
  }

  void func(long seed, Random p) {
    Random a = new Random(); // Noncompliant [[sc=20;ec=26]] {{Save and re-use this "Random".}}
    Random b = new Random(seed); // Compliant for Random(long seed)
    Object c = new Object();

    s = new Random();
    f = new Random();
    thid.f = new Random();

    f = a = new Random();
    a = b = new Random(); // false-negative, a and b are local variables, corner case limitation

    p = new Random(); // Noncompliant

    func(12, new Random());
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
