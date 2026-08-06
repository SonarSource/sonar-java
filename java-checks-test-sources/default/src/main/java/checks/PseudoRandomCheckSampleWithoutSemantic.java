package checks;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang.math.JVMRandom;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.RandomStringUtils;

import static org.apache.commons.lang.RandomStringUtils.random;

class PseudoRandomCheckSampleWithoutSemantic {
  void fun() {
    int i = nextInt();

    // Non static class, report only constructor
    // java.util.Random
    Random random = new Random(); // Noncompliant

    byte[] bytes = new byte[20];
    random.nextBytes(bytes); // Compliant

    // org.apache.commons.lang.math.JVMRandom

    // Static class, don't report constructor, only usage
    // java.lang.Math. Report only Math.random()
    double rand1 = Math.random(); // Noncompliant

    double abs = Math.abs(12); // Compliant

    // java.util.concurrent.ThreadLocalRandom
    int rand2 = ThreadLocalRandom.current().nextInt(); // Noncompliant

    // org.apache.commons.lang.math.RandomUtils
    RandomUtils randomUtils = new RandomUtils();

    // org.apache.commons.lang3.RandomUtils
    org.apache.commons.lang3.RandomUtils randomUtils2 = new org.apache.commons.lang3.RandomUtils();

    // org.apache.commons.lang.RandomStringUtils
    RandomStringUtils randomStringUtils = new RandomStringUtils();

    // Here we should raise an issue only on the `new Random`, not on the call to `random` itself
    String rand9 = RandomStringUtils.random(1, 0, 0, false, false, null, new Random()); // Noncompliant

    // Here there should be no issue because `random` will use the supplied secure source of randomness
    rand9 = RandomStringUtils.random(1, 0, 0, false, false, null, new SecureRandom()); // Compliant

    // org.apache.commons.lang3.RandomStringUtils
    org.apache.commons.lang3.RandomStringUtils randomStringUtils2 = new org.apache.commons.lang3.RandomStringUtils();

    // Here we should raise an issue only on the `new Random`, not on the call to `random` itself
    String rand11 = org.apache.commons.lang3.RandomStringUtils.random(1, 0, 0, false, false, null, new Random()); // Noncompliant

    // Here there should be no issue because `random` will use the supplied secure source of randomness
    rand11 = org.apache.commons.lang3.RandomStringUtils.random(1, 0, 0, false, false, null, new SecureRandom()); // Compliant

  }

  static String randomStringUtilsInstances(int value) {
    return switch (value) {
      case 0 -> org.apache.commons.lang3.RandomStringUtils.secureStrong().next(42); // Compliant
      case 42 -> org.apache.commons.lang3.RandomStringUtils.secure().next(42); // Compliant
      default -> "";
    };
  }

  int nextInt() {
    return 42;
  }
}
