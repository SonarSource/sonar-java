package checks;

import java.util.Random;
import java.lang.Math;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang.math.JVMRandom;
import org.apache.commons.lang.math.RandomUtils;

class RandomFloatToIntCheckSampleWithoutSemantic {
  //java.util.Random
  Random r = new Random();
  int rand = (int) r.nextDouble() * 50; // Noncompliant
  int rand2 = (int) r.nextFloat() * 50; // Noncompliant
  float rand3 = (float)r.nextFloat();
  int rand4 = (int) r.nextInt() * 50;
  int rand5 = (int)r.nextFloat(); // Noncompliant

  // java.lang.Math
  int rand6 = (int) Math.random() * 50; // Noncompliant
  int rand7 = (int) new RandomFloatToIntCheckSampleFooWS() {
    int foo() {
      int a = (int) Math.random() * 50; // Noncompliant
      return a;
    }
  }.foo();

  // java.util.concurrent.ThreadLocalRandom
  int rand8 = (int) ThreadLocalRandom.current().nextDouble() * 50; // Noncompliant
  int rand9 = (int) ThreadLocalRandom.current().nextDouble(1.0) * 50; // Noncompliant
  int rand10 = (int) ThreadLocalRandom.current().nextDouble(1.0, 2.0) * 50; // Noncompliant

  // org.apache.commons.lang.math.JVMRandom

  JVMRandom jvmRandom = new JVMRandom();
  float rand13 = (float)jvmRandom.nextFloat();
  int rand14 = (int) jvmRandom.nextInt() * 50;

  // org.apache.commons.lang.math.RandomUtils

  float rand18 = (float) RandomUtils.nextFloat();
  int rand19 = (int) RandomUtils.nextInt() * 50;

  // org.apache.commons.lang3.RandomUtils
  float rand23 = (float) org.apache.commons.lang3.RandomUtils.nextFloat();
  int rand24 = (int) org.apache.commons.lang3.RandomUtils.nextInt() * 50;

  void testLong(){
    int randInt = (int) r.nextDouble() * 50; // Noncompliant

    long randLong = (long) r.nextDouble() * 50; // Noncompliant

  }

}

class RandomFloatToIntCheckSampleFooWS {

}
