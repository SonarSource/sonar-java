package checks.tests;

import org.junit.Test;

class BadTestClassNameCheckCustom { // Noncompliant {{Rename class "BadTestClassNameCheckCustom" to match the regular expression: '^[A-Z][a-zA-Z0-9]*SonarTest$'}}
  @Test
  void foo() {}
}

class BadTestClassNameCheckCustomSonarTest { // Compliant
  @org.testng.annotations.Test
  void foo() {}
}

class BadTestClassNameCheckCustom2 { // Compliant
  void foo() {}

  BadTestClassNameCheckCustom a = new BadTestClassNameCheckCustom() {};
}
