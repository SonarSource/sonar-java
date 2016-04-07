import java.util.Random;
import java.lang.Math;

class A {

  Random r = new Random();
  int rand = (int) r.nextDouble() * 50;  // Noncompliant {{Use "java.util.Random.nextInt()" instead.}}
  int rand2 = (int) r.nextDouble() * 50;  // Noncompliant
  int rand3 = (int) Math.random() * 50;  // Noncompliant
  float rand4 = (float)r.nextFloat();
  int rand5 = (int) new Foo() {
    void foo() {
      int a = (int) Math.random() * 50;   // Noncompliant
    }
  };
  int rand6 = (int) r.nextInt() * 50;
}