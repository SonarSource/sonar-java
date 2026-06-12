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

// --- Digit-suffixed all-uppercase crypto acronym: documents a known tokenizer gap. ---
// `AES256_KEY` splits on `_` to ["AES256", "KEY"]. `AES256` has no lowercase letter so
// isAllUppercaseWithLetter returns true and it lowercases to `aes256` as a single token
// (NOT `aes` + `256`). The keyword set holds `aes`, not `aes256`, so no match fires.
// `KEY` -> `[key]`, which is not in the Java security-keyword set.
// Intentional: splitting letters from trailing digits is out of scope here; tracked under APPSEC-3004.
  void digitSuffixedAcronym() {
    final int AES256_KEY = 32;
    Random r = new Random(); // Compliant
    r.nextInt(AES256_KEY);
  }
}

// --- Inner-class scope isolation. ---
// The outer class name `TokenAware` contains the security keyword `token`, but the
// PRNG call lives in an inner class with neutral identifiers. findDeclarationScope
// returns the inner ClassTree first, so the outer's keyword is NOT in scope.
class TokenAware {
  static class NeutralInner {
    static final Random R = new Random(); // Compliant
  }
}

