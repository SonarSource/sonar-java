package checks;

import java.util.Random;
import java.lang.Math;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang.math.JVMRandom;
import org.apache.commons.lang.math.RandomUtils;

class RandomFloatToIntCheckSample {
  //java.util.Random
  Random r = new Random();
  int rand = (int) r.nextDouble() * 50; // Noncompliant {{Use "nextInt()" instead.}}
  int rand2 = (int) r.nextFloat() * 50; // Noncompliant
  float rand3 = (float)r.nextFloat();
  int rand4 = (int) r.nextInt() * 50;
  int rand5 = (int)r.nextFloat(); // Noncompliant

  // java.lang.Math
  int rand6 = (int) Math.random() * 50; // Noncompliant {{Use "java.util.Random.nextInt()" instead.}}
  int rand7 = (int) new RandomFloatToIntCheckSampleFoo() {
    int foo() {
      int a = (int) Math.random() * 50; // Noncompliant
      return a;
    }
  }.foo();

  // java.util.concurrent.ThreadLocalRandom
  int rand8 = (int) ThreadLocalRandom.current().nextDouble() * 50; // Noncompliant {{Use "nextInt()" instead.}}
  int rand9 = (int) ThreadLocalRandom.current().nextDouble(1.0) * 50; // Noncompliant
  int rand10 = (int) ThreadLocalRandom.current().nextDouble(1.0, 2.0) * 50; // Noncompliant

  // org.apache.commons.lang.math.JVMRandom

  JVMRandom jvmRandom = new JVMRandom();
  int rand11 = (int) jvmRandom.nextDouble() * 50; // Noncompliant {{Use "nextInt()" instead.}}
  int rand12 = (int) jvmRandom.nextFloat() * 50; // Noncompliant
  float rand13 = (float)jvmRandom.nextFloat();
  int rand14 = (int) jvmRandom.nextInt() * 50;
  int rand15 = (int)jvmRandom.nextFloat(); // Noncompliant

  // org.apache.commons.lang.math.RandomUtils

  int rand16 = (int) RandomUtils.nextDouble() * 50; // Noncompliant {{Use "nextInt()" instead.}}
  int rand17 = (int) RandomUtils.nextFloat() * 50; // Noncompliant
  float rand18 = (float) RandomUtils.nextFloat();
  int rand19 = (int) RandomUtils.nextInt() * 50;
  int rand20 = (int) RandomUtils.nextFloat(); // Noncompliant

  // org.apache.commons.lang3.RandomUtils
  int rand21 = (int) org.apache.commons.lang3.RandomUtils.nextDouble() * 50; // Noncompliant {{Use "nextInt()" instead.}}
  int rand22 = (int) org.apache.commons.lang3.RandomUtils.nextFloat() * 50; // Noncompliant
  float rand23 = (float) org.apache.commons.lang3.RandomUtils.nextFloat();
  int rand24 = (int) org.apache.commons.lang3.RandomUtils.nextInt() * 50;
  int rand25 = (int)org.apache.commons.lang3.RandomUtils.nextFloat(); // Noncompliant

  void testLong(){
    int randInt = (int) r.nextDouble() * 50; // Noncompliant
//                      ^^^^^^^^^^^^
    long randLong = (long) r.nextDouble() * 50; // Noncompliant {{Use "nextLong()" instead.}}
//                         ^^^^^^^^^^^^
  }

}

class RandomFloatToIntCheckSampleFoo {

}
