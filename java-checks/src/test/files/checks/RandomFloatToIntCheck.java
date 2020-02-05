import java.util.Random;
import java.lang.Math;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang.math.JVMRandom;
import org.apache.commons.lang.math.RandomUtils;

class A {
  //java.util.Random
  Random r = new Random();
  int rand = (int) r.nextDouble() * 50;  // Noncompliant {{Use "nextInt()" instead.}}
  int rand2 = (int) r.nextFloat() * 50;  // Noncompliant
  float rand3 = (float)r.nextFloat();
  int rand4 = (int) r.nextInt() * 50;
  int rand5 = (int)r.nextFloat(); // Noncompliant; will always be 0;

  // java.lang.Math
  int rand6 = (int) Math.random() * 50;  // Noncompliant {{Use "java.util.Random.nextInt()" instead.}}
  int rand7 = (int) new Foo() {
    int foo() {
      int a = (int) Math.random() * 50;   // Noncompliant
      return a;
    }
  }.foo();

  // java.util.concurrent.ThreadLocalRandom
  int rand8 = (int) ThreadLocalRandom.current().nextDouble() * 50;  // Noncompliant {{Use "nextInt()" instead.}}
  int rand9 = (int) ThreadLocalRandom.current().nextDouble(1.0) * 50;  // Noncompliant
  int rand10 = (int) ThreadLocalRandom.current().nextDouble(1.0, 2.0) * 50;  // Noncompliant

  // org.apache.commons.lang.math.JVMRandom

  JVMRandom jvmRandom = new JVMRandom();
  int rand11 = (int) jvmRandom.nextDouble() * 50;  // Noncompliant {{Use "nextInt()" instead.}}
  int rand12 = (int) jvmRandom.nextFloat() * 50;  // Noncompliant
  float rand13 = (float)jvmRandom.nextFloat();
  int rand14 = (int) jvmRandom.nextInt() * 50;
  int rand15 = (int)jvmRandom.nextFloat(); // Noncompliant; will always be 0;

  // org.apache.commons.lang.math.RandomUtils

  RandomUtils randomUtils = new RandomUtils();
  int rand16 = (int) randomUtils.nextDouble() * 50;  // Noncompliant {{Use "nextInt()" instead.}}
  int rand17 = (int) randomUtils.nextFloat() * 50;  // Noncompliant
  float rand18 = (float)randomUtils.nextFloat();
  int rand19 = (int) randomUtils.nextInt() * 50;
  int rand20 = (int)randomUtils.nextFloat(); // Noncompliant; will always be 0;

  // org.apache.commons.lang3.RandomUtils
  org.apache.commons.lang3.RandomUtils randomUtils2 = new org.apache.commons.lang3.RandomUtils();
  int rand21 = (int) randomUtils2.nextDouble() * 50;  // Noncompliant {{Use "nextInt()" instead.}}
  int rand22 = (int) randomUtils2.nextFloat() * 50;  // Noncompliant
  float rand23 = (float)randomUtils2.nextFloat();
  int rand24 = (int) randomUtils2.nextInt() * 50;
  int rand25 = (int)randomUtils2.nextFloat(); // Noncompliant; will always be 0;
}

class Foo {
}
