package checks;

import java.util.Random;

// SONARJAVA-6440: no crypto import. Random call appears outside any method (field initializer
// and static initializer). Scope falls back to the enclosing class; the class identifiers
// drive the keyword check.

class PseudoRandomCheckFieldScopeSampleSecure {
  // Class name contains keyword: `TokenGenerator` tokens -> [token, generator]. `token` matches.
  static class TokenGenerator {
    static final Random RNG = new Random(); // Noncompliant {{Make sure that using this pseudorandom number generator is safe here.}}
  }

  // No method scope, but a sibling field name contains a security keyword.
  static class Holder {
    String password;
    static final Random R = new Random(); // Noncompliant
  }

  // Static initializer: no enclosing method; class identifiers carry the keyword.
  static class CipherBundle {
    static Random r;
    static {
      r = new Random(); // Noncompliant
    }
  }
}

class PseudoRandomCheckFieldScopeSampleNeutral {
  // No method, no security keyword anywhere in the enclosing class -> Compliant.
  static final Random R = new Random(); // Compliant
}
