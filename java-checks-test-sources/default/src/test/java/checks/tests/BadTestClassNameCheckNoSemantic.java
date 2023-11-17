package checks.tests;

import org.junit.Test;

class BadTestClassNameCheckNoSemantic { // Noncompliant {{Rename class "BadTestClassNameCheckNoSemantic" to match the regular expression: '^((Test|IT)[a-zA-Z0-9_]+|[A-Z][a-zA-Z0-9_]*(Test|Tests|TestCase|IT|ITCase))$'}}
  @org.junit.Test // resolved because fully qualified name is used
  void foo() {}
}

class BadTestClassNameCheckNoSemantic3 { // Noncompliant
  @org.junit.jupiter.api.Test // resolved because fully qualified name is used
  void foo() {}
}

class BadTestClassNameCheckNoSemantic2 { // Compliant
  @Test // not resolved without semantic
  void foo() {}
}

abstract class AbstractBadTestClassNameCheckNoSemantic { // compliant
  @org.junit.Test
  void foo() {}
}
