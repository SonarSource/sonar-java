package checks;

import java.util.Random;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

// SONARJAVA-6440 Part 1: file imports `org.bouncycastle.*` -> all PRNG calls flagged
// regardless of any per-scope keyword check.
class PseudoRandomCheckCryptoImportSample {

  // BouncyCastle reference so the import is used.
  static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

  void noKeywordsInScope() {
    int counter = 0;
    Random r = new Random(); // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
    counter += r.nextInt();
  }

  double anotherNeutralMethod() {
    return Math.random(); // Noncompliant
  }
}
