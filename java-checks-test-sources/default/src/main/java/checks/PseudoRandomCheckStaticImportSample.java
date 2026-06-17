package checks;

import static javax.crypto.Cipher.ENCRYPT_MODE;

import java.util.Random;

// SONARJAVA-6440 Part 1: static import of a crypto class still parses as Tree.Kind.IMPORT,
// so the concatenated name `javax.crypto.Cipher.ENCRYPT_MODE` matches the `javax.crypto.`
// prefix and the file-level crypto-import gate fires.
class PseudoRandomCheckStaticImportSample {

  static final int MODE = ENCRYPT_MODE; // retain the static import

  void noKeywordsInScope() {
    int counter = 0;
    Random r = new Random(); // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
    counter += r.nextInt();
  }
}
