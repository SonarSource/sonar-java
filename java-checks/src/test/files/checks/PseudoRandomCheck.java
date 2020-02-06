import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang.math.JVMRandom;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.RandomStringUtils;

class A {
  void fun() {
    int i = nextInt();

    // java.util.Random. Report only constructor
    Random random = new Random(); // Noncompliant [[sc=25;ec=31]] {{Make sure that using this pseudorandom number generator is safe here.}}
    byte[] bytes = new byte[20];
    random.nextBytes(bytes); // Compliant

    // java.lang.Math. Report only Math.random()
    double rand1 = Math.random(); // Noncompliant [[sc=25;ec=31]]
    double abs = Math.abs(12); // Compliant

    // java.util.concurrent.ThreadLocalRandom
    int rand2 = ThreadLocalRandom.current().nextInt();  // Noncompliant

    // org.apache.commons.lang.math.JVMRandom
    JVMRandom jvmRandom = new JVMRandom(); // Noncompliant
    double rand3 = jvmRandom.nextDouble(); // Noncompliant

    // org.apache.commons.lang.math.RandomUtils
    RandomUtils randomUtils = new RandomUtils(); // Noncompliant
    float rand4 = randomUtils.nextFloat(); // Noncompliant
    float rand5 = RandomUtils.nextFloat(); // Noncompliant

    // org.apache.commons.lang3.RandomUtils
    org.apache.commons.lang3.RandomUtils randomUtils2 = new org.apache.commons.lang3.RandomUtils(); // Noncompliant
    float rand6 = randomUtils2.nextFloat(); // Noncompliant
    float rand7 = org.apache.commons.lang3.RandomUtils.nextFloat(); // Noncompliant

    // org.apache.commons.lang.RandomStringUtils
    RandomStringUtils randomStringUtils = new RandomStringUtils(); // Noncompliant
    String rand8 = randomStringUtils.random(1); // Noncompliant
    String rand9 = RandomStringUtils.random(1); // Noncompliant

    // org.apache.commons.lang3.RandomStringUtils
    org.apache.commons.lang3.RandomStringUtils randomStringUtils2 = new org.apache.commons.lang3.RandomStringUtils(); // Noncompliant
    String rand10 = randomStringUtils.random(1); // Noncompliant
    String rand11 = org.apache.commons.lang3.RandomStringUtils.random(1); // Noncompliant

  }

  int nextInt() {
    return 42;
  }
}
