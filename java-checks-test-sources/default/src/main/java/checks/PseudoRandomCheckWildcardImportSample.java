package checks;

import java.security.*;
import java.util.Random;

// SONARJAVA-6440 Part 1: wildcard import `java.security.*` -> all PRNG calls flagged.
// ExpressionsHelper.concatenate returns "java.security.*", which still starts with the
// "java.security." prefix, so the file-level crypto-import check fires.
class PseudoRandomCheckWildcardImportSample {

  static final KeyPair PROVIDER_KEYPAIR = null; // forces the wildcard import to be retained

  void noKeywordsInScope() {
    int counter = 0;
    Random r = new Random(); // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
    counter += r.nextInt();
  }

  double anotherNeutralMethod() {
    return Math.random(); // Noncompliant
  }
}
