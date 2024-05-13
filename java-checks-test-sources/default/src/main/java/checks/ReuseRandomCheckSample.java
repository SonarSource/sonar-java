package checks;

import java.util.Random;

public class ReuseRandomCheckSample {

  static Random staticField = new Random(); // Compliant for static field
  Random field = new Random(); // Compliant for field

  ReuseRandomCheckSample() {
    Random localVar = new Random(); // Compliant in constructor
    new Random();
  }

  void func(long seed, Random param) {
    Random localVar1 = new Random(); // Noncompliant {{Save and re-use this "Random".}}
//                         ^^^^^^
    Random localVar2 = new Random(seed); // Compliant for Random(long seed)
    Object localVar3 = new Object();

    staticField = new Random();
    field = new Random();
    this.field = new Random();

    field = localVar1 = new Random();
    field = (localVar1 = new Random());
    localVar1 = localVar2 = new Random(); // Noncompliant
    localVar1 = (localVar2 = new Random()); // Noncompliant
    Random localVar4 = field = new Random();
    Random localVar5 = localVar4 = new Random(); // Noncompliant
    new Random(); // Noncompliant

    param = new Random(); // Noncompliant

    func(12, new Random());
    func(12, localVar1 = new Random());
    int usedDirectly = new Random().nextInt(); // Noncompliant
//                         ^^^^^^
    (new Random()).nextInt(); // Noncompliant
  }

  public static void main(String[] args) {
    Random localVar = new Random(); // Compliant in "main()"
  }

  public class B {
    /*not static*/ void main() {
      Random localVar = new Random(); // Noncompliant
    }
  }

}
