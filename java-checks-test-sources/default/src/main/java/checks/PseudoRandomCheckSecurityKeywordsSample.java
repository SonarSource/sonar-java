package checks;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;

// SONARJAVA-6440: no crypto import. Security keywords reached via per-scope identifier scan.
class PseudoRandomCheckSecurityKeywordsSample {

  // --- Method-scope keyword tokenized from camelCase (`userPassword` -> [user, password]). ---
  void camelCaseLocal() {
    String userPassword = "x";
    Random r = new Random(); // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
    r.nextInt();
  }

  // --- Method-scope keyword tokenized from snake_case (`user_token` -> [user, token]). ---
  void snakeCaseLocal() {
    String user_token = "x";
    double d = Math.random(); // Noncompliant
  }

  // --- Method-scope keyword from all-uppercase (`HMAC` -> [hmac]). ---
  void upperCaseLocal() {
    final int HMAC = 32;
    int v = ThreadLocalRandom.current().nextInt(); // Noncompliant
  }

  // --- Keyword in the method name itself. ---
  void encryptPayload() {
    float f = RandomUtils.nextFloat(); // Noncompliant
  }

  // --- Keyword in a parameter name. ---
  String randomFromToken(String token) {
    return RandomStringUtils.random(1); // Noncompliant
  }

  // --- Whole-identifier match (`password`). ---
  void wholeIdentifierMatch() {
    String password = "x";
    Random r = new Random(); // Noncompliant
  }

  // --- No keyword in scope: must NOT be flagged (heuristic suppresses it). ---
  void unrelatedScope() {
    int counter = 0;
    Random r = new Random(); // Compliant
    counter += r.nextInt();
  }

  // --- camelCase that DOES NOT match keyword after tokenization. ---
  // `randomBytes` tokenizes to [random, bytes]; neither is in the keyword set
  // (the keyword `randombytes` only matches an all-lowercase or all-uppercase literal).
  void splitRandomBytes() {
    byte[] randomBytes = new byte[16];
    Random r = new Random(); // Compliant
    r.nextBytes(randomBytes);
  }
}
