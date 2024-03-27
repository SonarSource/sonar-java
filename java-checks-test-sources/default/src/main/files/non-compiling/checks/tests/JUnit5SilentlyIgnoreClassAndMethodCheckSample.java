package checks;

import org.junit.jupiter.api.Test;

class JUnit5SilentlyIgnoreClassAndMethodCheckSample {

  @Test
  int testReturningInt() { return 0; } // Noncompliant

  @Test
  UnknownType testReturningUnknownType() { return null; } // Compliant, return type semantic is missing
}
