package checks;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang.math.JVMRandom;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.RandomStringUtils;

// SONARJAVA-6440: no crypto import, no security-keyword identifiers in scope ->
// the security-context heuristic suppresses the issue.
class PseudoRandomCheckNoContextSample {

  void neutralMethod() {
    Random r = new Random(); // Compliant
    int v = r.nextInt();

    JVMRandom j = new JVMRandom(); // Compliant
    double d1 = j.nextDouble();

    double d2 = Math.random(); // Compliant
    int v2 = ThreadLocalRandom.current().nextInt(); // Compliant

    RandomUtils ru = new RandomUtils();
    float f1 = RandomUtils.nextFloat(); // Compliant

    RandomStringUtils rsu = new RandomStringUtils();
    String s1 = RandomStringUtils.random(1); // Compliant
  }

  int doWork(int input) {
    return input + 1;
  }
}
